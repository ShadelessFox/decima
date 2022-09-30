package com.shade.decima.ui.data.viewer.mesh.dmf;

public class DMFExternalBuffer extends DMFBuffer {
    public String bufferFileName;

    public DMFExternalBuffer(String bufferFileName, int bufferSize) {
        this.bufferFileName = bufferFileName;
        this.bufferSize = bufferSize;
    }
}
