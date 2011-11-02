package org.one.stone.soup.screen.recorder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FramePacket {

	private int frameSize;
	private int compressedFrameSize;
	private int unzBytesSize;
	private int[] currentFrame;
	private byte[] compressedFrame;
	private int[] previousFrame;
	
	public void setCompressedFrame(byte[] compressedFrame) {
		System.arraycopy(compressedFrame, 0, this.compressedFrame, 0, compressedFrame.length);
	}

	public FramePacket(int frameSize)
	{
		this.frameSize = frameSize;
		
		resetPreviousFrame();
		compressedFrame = new byte[frameSize*4];
		currentFrame = new int[frameSize];
	}
	
	public void prepareFrame()
	{
		previousFrame = currentFrame;
	}
	
	public int getCompressedFrameSize() {
		return compressedFrameSize;
	}

	public void setCompressedFrameSize(int compressedFrameSize) {
		this.compressedFrameSize = compressedFrameSize;
	}
	
	public int getUnzBytesSize() {
		return unzBytesSize;
	}

	public void setUnzBytesSize(int unzBytesSize) {
		this.unzBytesSize = unzBytesSize;
	}

	public int getDataSize()
	{
		return currentFrame.length;
	}
	
	public int[] getCurrentFrame() {
		return currentFrame;
	}

	public int[] getPreviousFrame() {
		return previousFrame;
	}

	public byte[] getCompressedFrame() {
		return compressedFrame;
	}

	public void setCurrentFrame(int[] currentFrame) {
		this.currentFrame = currentFrame;
	}

	public int getFrameSize() {
		return frameSize;
	}

	public void resetPreviousFrame() {
		previousFrame = new int[frameSize];
	}
	
	public void write(OutputStream out,int timestamp) throws IOException {
		
		out.write( (timestamp & 0xFF000000) >>>24 );
		out.write( (timestamp & 0x00FF0000) >>>16 );
		out.write( (timestamp & 0x0000FF00) >>>8 );
		out.write( (timestamp & 0x000000FF) );
		
		if(compressedFrameSize==0)
		{
			out.write(0);
			return;
		}
		else
		{
			out.write(1);			
		}
		
		// TODO remove this TESTING CODE
		System.out.println("Uncompressed frame data : " + (frameSize * 4)+ " bytes");
		System.out.println("RLE encoded data size   : " + unzBytesSize);
		System.out.println("Gzipped frame data      : " + compressedFrameSize + " bytes");
		
		// write out the new frame data length before it was gzipped
		out.write( (unzBytesSize & 0xFF000000) >>>24 );
		out.write( (unzBytesSize & 0x00FF0000) >>>16 );
		out.write( (unzBytesSize & 0x0000FF00) >>>8 );
		out.write( (unzBytesSize & 0x000000FF) );
		
		out.write( (compressedFrameSize & 0xFF000000) >>>24 );
		out.write( (compressedFrameSize & 0x00FF0000) >>>16 );
		out.write( (compressedFrameSize & 0x0000FF00) >>>8 );
		out.write( (compressedFrameSize & 0x000000FF) );
		
		out.write( compressedFrame,0,compressedFrameSize );
		out.flush();
	}
	
	public int read(InputStream in) throws IOException {
		
		int i = in.read();
		int time = i;
		time = time << 8;
		i = in.read();
		time += i;
		time = time << 8;
		i = in.read();
		time += i;
		time = time << 8;
		i = in.read();
		time += i;

		byte type = (byte)in.read();
		if(type==0)
		{
			return time;
		}
		
		// read the number of bytes in new frame data before it was gzipped
		int r = in.read();
		int unz = r;
		unz = unz << 8;
		r = in.read();
		unz += r;
		unz = unz << 8;
		r = in.read();
		unz += r;
		unz = unz << 8;
		r = in.read();
		unz += r;
		unzBytesSize = unz;
		

		i = in.read();
		int zSize = i;
		zSize = zSize << 8;
		i = in.read();
		zSize += i;
		zSize = zSize << 8;
		i = in.read();
		zSize += i;
		zSize = zSize << 8;
		i = in.read();
		zSize += i;
		
		int readCursor = 0;
		int sizeRead = 0;
		
		while(sizeRead>-1)
		{
			readCursor+=sizeRead;
			if(readCursor>=zSize)
			{
				break;
			}
			
			sizeRead = in.read(compressedFrame,readCursor,zSize-readCursor);			
		}
		
		compressedFrameSize = zSize;
		return time;
	}
}