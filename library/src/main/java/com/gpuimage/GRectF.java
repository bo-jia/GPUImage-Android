package com.gpuimage;

public class GRectF {

    public float x		= 0;
    public float y		= 0;
    public float width	= 0;
    public float height	= 0;

    public GRectF() {}

    public GRectF(GRectF rect) {
    	if (rect == null) {
    		return;
		}

    	x = rect.x;
    	y = rect.y;
    	width = rect.width;
    	height = rect.height;
    }
    public GRectF(float x, float y, float width, float height) {
    	this.x = x;
    	this.y = y;
    	this.width = width;
    	this.height = height;
    }
	@Override
	public String toString() {
		return "GRectF [x=" + x + ", y=" + y + ", width=" + width + ", height="
				+ height + "]";
	}

	@Override
	public boolean equals(Object obj) {
    	if (this == obj) {
    		return true;
		}
    	if (obj == null || getClass() != obj.getClass()) {
    		return false;
		}

		GRectF o = (GRectF)obj;

		if (Float.compare(o.x, x) != 0) {
			return false;
		}
		if (Float.compare(o.y, y) != 0) {
			return false;
		}
		if (Float.compare(o.width, width) != 0) {
			return false;
		}
		if (Float.compare(o.height, height) != 0) {
			return false;
		}

		return true;
	}
}
