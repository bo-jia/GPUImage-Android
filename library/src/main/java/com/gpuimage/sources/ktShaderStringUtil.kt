package com.gpuimage.sources

/**
 * @ClassName
 * @Description
 * @Author danny
 * @Date 2018/1/17 14:35
 */
class ShaderStringUtil {
    companion object {
        var kGPUImageSandwichFragmentShader = """
            precision mediump float;
            varying highp vec2 textureCoordinate;
            varying highp vec2 textureCoordinate2;
            varying highp vec2 textureCoordinate3;

            uniform sampler2D inputImageTexture;
            uniform sampler2D inputImageTexture2;
            uniform sampler2D inputImageTexture3;

            void main()
            {
                vec4 backgroundInputColor = texture2D(inputImageTexture, textureCoordinate);
                vec4 foregroundInputColor = texture2D(inputImageTexture2, textureCoordinate2);
                vec4 maskInputColor = texture2D(inputImageTexture3, textureCoordinate3);

                float a = max(maskInputColor.r, max(maskInputColor.g, maskInputColor.b));
                gl_FragColor = vec4(backgroundInputColor.rgb * (1.0 - a) + foregroundInputColor.rgb, 1.0);
            }
        """;

         val kGPUImageOES2ViewFShader = """
                #extension GL_OES_EGL_image_external : require
                varying highp vec2 textureCoordinate;
                uniform samplerExternalOES inputImageTexture;
                void main()
                {
                    gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
                }
         """;
    }
}