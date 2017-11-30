package GPUImage;

public interface GPUImageInput {
	void setInputSize(GSize newSize, int textureIndex);
	void newFrameReadyAtTime(final double frameTime, final int texIndex);
	void setInputFramebuffer(GPUImageFramebuffer newInputFramebuffer, int texIndex);
	void setInputRotation(GPUImageContext.GPUImageRotationMode newInputRotation, int textureIndex);
	GSize maximumOutputSize();
	void endProcessing();
	boolean shouldIgnoreUpdatesToThisTarget();
	boolean enabled();
	boolean wantsMonochromeInput();
	void setCurrentlyReceivingMonochromeInput(boolean newValue);
	int nextAvailableTextureIndex();
}
