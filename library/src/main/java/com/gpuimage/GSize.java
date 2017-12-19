package com.gpuimage;

public class GSize {
	public int width  = 0;
	public int height = 0;

	public final static GSize Zero = new GSize();

	public static GSize newZero() { return new GSize(); }

	public GSize() {}

	public GSize(int width, int height) {
        this.width = width;
        this.height = height;
	}

	public GSize(GSize size) {
		if (size == null) return;

		this.width = size.width;
		this.height = size.height;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;

		if (obj instanceof GSize) {
			GSize other = (GSize) obj;
			return width == other.width && height == other.height;
		}
		return false;
	}

	@Override
	public String toString() {
		return "GSize [width=" + width + ", height=" + height + "]";
	}

}
