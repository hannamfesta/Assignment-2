package app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import ac.ArithmeticEncoder;
import io.OutputStreamBitSink;

public class ACEncodeVideoFile {
	public static void main(String[] args) throws IOException {
		String input_file_name = "data/out.dat";
		String output_file_name = "data/video-compressed.dat";

		int range_bit_width = 40;

		System.out.println("Encoding video file: " + input_file_name);
		System.out.println("Output file: " + output_file_name);
		System.out.println("Range Register Bit Width: " + range_bit_width);

		int num_pixels = (int) new File(input_file_name).length();
		int num_frames = 300;
		int pixels_per_frame = num_pixels/num_frames; 
				
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
		
		Integer[] previous_frame = new Integer[pixels_per_frame];

		//initializes all intensities of previous frame to black 
		//- in the first frame all pixels will be encoded with model 0
		for(int i=0; i<pixels_per_frame; i++){
			previous_frame[i]= 0;
		}
		
		//outer loop that goes through each frame of the input 
		//inner loop goes through each pixel of frame 
			//encode based on model for intensity of pixel in previous frame in same x,y position
		
		for (int i=0; i<num_frames; i++) {
			for(int j=0; j<pixels_per_frame; j++ ){
				int intensity = fis.read();
				FreqCountIntegerSymbolModel model = models[previous_frame[j]];
				encoder.encode(intensity, model, bit_sink);
				//update model used
				model.addToCount(intensity);
				//update previous frame to be this pixel intensity value 
				previous_frame[j]=intensity;
			}
		}
		fis.close();

		encoder.emitMiddle(bit_sink);
		bit_sink.padToWord();
		fos.close();
		
		System.out.println("Done");
	}
}
