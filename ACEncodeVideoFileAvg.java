package app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import ac.ArithmeticEncoder;
import app.FreqCountIntegerSymbolModel;
import io.OutputStreamBitSink;

public class ACEncodeVideoFileAvg {
	public static void main(String[] args) throws IOException {
		String input_file_name = "data/out.dat";
		String output_file_name = "data/videoAvg-compressed.dat";

		int range_bit_width = 40;

		System.out.println("Encoding video file: " + input_file_name);
		System.out.println("Output file: " + output_file_name);
		System.out.println("Range Register Bit Width: " + range_bit_width);

		int num_pixels = (int) new File(input_file_name).length();
		int num_frames = 300;
		int pixels_per_frame = num_pixels/num_frames; 
		int dim = (int) Math.sqrt((double)pixels_per_frame);
				
		Integer[] intensities = new Integer[256];
		for (int i=0; i<256; i++) {
			intensities[i] = i;
		}

		//make model for each pixel intensity - 256 total
		FreqCountIntegerSymbolModel[] models = new FreqCountIntegerSymbolModel[256];
		
		for (int i=0; i<256; i++) {
			models[i] = new FreqCountIntegerSymbolModel(intensities);
		}
		
		ArithmeticEncoder<Integer> encoder = new ArithmeticEncoder<Integer>(range_bit_width);

		FileOutputStream fos = new FileOutputStream(output_file_name);
		OutputStreamBitSink bit_sink = new OutputStreamBitSink(fos);

		//4 bytes are the number of pixels encoded
		bit_sink.write(num_pixels, 32);		
		
		//4 bytes are the number of frames
		bit_sink.write(num_frames, 32);
				
		//width of the range registers
		bit_sink.write(range_bit_width, 8);

		FileInputStream fis = new FileInputStream(input_file_name);
		
		Integer[][] previous_frame = new Integer[64][64];

		//initializes all intensities of previous frame to black 
		//- in the first frame all pixels will be encoded with model 0
		for(int i=0; i<dim; i++){
			for(int j=0; j<dim; j++){
				previous_frame[i][j]= 0;
			}
		}
		
		//outer loop that goes through each frame of the input 
		//inner loop goes through each pixel of frame 
		for (int i=0; i<num_frames; i++) {
			for(int j=0; j<dim; j++ ){
				for(int k=0; k<dim; k++){
					//calculate avg intensity 
					int avg_intensity = 0;
					int pixelCount = 0;
					for(int m=j-1; m<=j+1; m++){
						for(int n=k-1; n<=k+1; n++){
							if(m>=0 && m<dim){
								if(n>=0 && n<dim){
									avg_intensity = avg_intensity+previous_frame[m][n];
									pixelCount++;
								}
							}
						}
					}
					avg_intensity = avg_intensity/pixelCount;
					int intensity = fis.read();
					FreqCountIntegerSymbolModel model = models[avg_intensity];
					encoder.encode(intensity, model, bit_sink);
					//update model used
					model.addToCount(intensity);
					//update previous frame to be this pixel intensity value 
					previous_frame[j][k]=intensity;
				}
			}
		}
		fis.close();

		// emit middle pattern and pad to the next word
		encoder.emitMiddle(bit_sink);
		bit_sink.padToWord();
		fos.close();
		
		System.out.println("Done");
	}
}

