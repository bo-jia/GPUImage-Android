package com.gpuimage.sources;

/**
 * Created by j on 4/12/2017.
 */

public class GPUImageColorConversion {
    public final static float[] kColorConversion601Default = {
            1.164f,  1.164f, 1.164f,
            0.0f, -0.392f, 2.017f,
            1.596f, -0.813f,   0.0f
    };

    public final static float[] kColorConversion601FullRangeDefault = {
            1.0f,    1.0f,    1.0f,
            0.0f,    -0.343f, 1.765f,
            1.4f,    -0.711f, 0.0f,
    };

    public final static float[] kColorConversion709Default = {
            1.164f,  1.164f, 1.164f,
            0.0f, -0.213f, 2.112f,
            1.793f, -0.533f,   0.0f,
    };

    public final static String kGPUImageYUVFullRangeConversionForLAFragmentShaderString = "" +
            " varying highp vec2 textureCoordinate;\n" +

            " uniform sampler2D luminanceTexture;\n" +
            " uniform sampler2D chrominanceTexture;\n" +
            " uniform mediump mat3 colorConversionMatrix;\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     mediump vec3 yuv;\n" +
            "     lowp vec3 rgb;\n" +
            "     \n" +
            "     yuv.x = texture2D(luminanceTexture, textureCoordinate).r;\n" +
            "     yuv.yz = texture2D(chrominanceTexture, textureCoordinate).ra - vec2(0.5, 0.5);\n" +
            "     rgb = colorConversionMatrix * yuv;\n" +
            "     \n" +
            "     gl_FragColor = vec4(rgb, 1);\n" +
            " }";
}
