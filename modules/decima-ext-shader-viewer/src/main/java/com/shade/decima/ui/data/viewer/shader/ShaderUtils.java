package com.shade.decima.ui.data.viewer.shader;

import com.shade.decima.model.rtti.types.java.HwShader;
import com.shade.decima.ui.data.viewer.shader.ffm.D3DCompiler;
import com.shade.decima.ui.data.viewer.shader.ffm.DXCompiler;
import com.shade.decima.ui.data.viewer.shader.ffm.IDxcCompiler;
import com.shade.decima.ui.data.viewer.shader.ffm.IDxcUtils;
import com.shade.decima.ui.data.viewer.shader.settings.ShaderViewerSettings;
import com.shade.util.NotNull;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.util.Objects;

final class ShaderUtils {
    private ShaderUtils() {
    }

    @NotNull
    static String decompile(@NotNull HwShader.Entry entry) {
        if (entry.shaderModel() > 5) {
            return decompileDXIL(entry.program().blob());
        } else {
            return decompileDXBC(entry.program().blob());
        }
    }

    @NotNull
    private static String decompileDXIL(@NotNull byte[] data) {
        try (Arena arena = Arena.ofConfined()) {
            String path = Objects.requireNonNullElse(ShaderViewerSettings.getInstance().dxCompilerPath, "dxcompiler.dll");
            SymbolLookup lookup;

            try {
                lookup = SymbolLookup.libraryLookup(path, arena);
            } catch (Exception e) {
                throw new IllegalStateException("Can't find DirectX compiler library. You can specify path to the compiler in File | Settings | Core Editor | Shader Viewer.", e);
            }

            var compiler = new DXCompiler(lookup);

            try (
                var dxcUtils = compiler.createInstance(DXCompiler.CLSID_DxcUtils, IDxcUtils.IID_IDxcUtils);
                var dxcCompiler = compiler.createInstance(DXCompiler.CLSID_DxcCompiler, IDxcCompiler.IID_IDxcCompiler);
                var sourceBlob = dxcUtils.createBlob(data, 0);
                var disassemblyBlob = dxcCompiler.disassemble(sourceBlob);
                var disassemblyBlobUtf8 = dxcUtils.getBlobAsUtf8(disassemblyBlob);
            ) {
                return disassemblyBlobUtf8.getString();
            }
        }
    }

    @NotNull
    private static String decompileDXBC(@NotNull byte[] data) {
        try (Arena arena = Arena.ofConfined()) {
            String path = Objects.requireNonNullElse(ShaderViewerSettings.getInstance().d3dCompilerPath, "d3dcompiler_47.dll");
            SymbolLookup lookup;

            try {
                lookup = SymbolLookup.libraryLookup(path, arena);
            } catch (Exception e) {
                throw new IllegalStateException("Can't find Direct3D compiler library. You can specify path to the compiler in File | Settings | Core Editor | Shader Viewer.", e);
            }

            var compiler = new D3DCompiler(lookup);

            try (var blob = compiler.disassemble(data, 0, null)) {
                return blob.getBuffer().getString(0);
            }
        }
    }
}
