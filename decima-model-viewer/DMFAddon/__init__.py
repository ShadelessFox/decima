import bpy

from DMFAddon.operators import DMF_OT_DMFImport

bl_info = {
    "name": "DMF Import plugin",
    "author": "REDxEYE",
    "version": (0, 0, 2),
    "blender": (3, 0, 0),
    "description": "Import `Decima Workshop` dmf files",
    "category": "Import-Export"
}

ALL_CLASSES = (DMF_OT_DMFImport,)

register_, unregister_ = bpy.utils.register_classes_factory(ALL_CLASSES)


def menu_import(self, context):
    self.layout.operator(DMF_OT_DMFImport.bl_idname, text="Decima Model (.dmf)")


def register():
    register_()
    bpy.types.TOPBAR_MT_file_import.append(menu_import)


def unregister():
    bpy.types.TOPBAR_MT_file_import.remove(menu_import)
    unregister_()
