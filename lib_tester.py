import json
from pathlib import Path

import bpy

from DMFAddon.dmflib.scene import DMFSceneFile
from DMFAddon.importer import import_dmf

dmf_path = Path(r"C:\Users\AORUS\Documents\tile_-1_-1_water.core.dmf")
with dmf_path.open('r') as f:
    data = json.load(f)
    res = DMFSceneFile.from_json(data)
    res.set_buffers_path(dmf_path.parent / 'dbuffers')
    # print(res)
    # print(json.dumps(res, default=json_default, indent=1))
    for obj in bpy.data.objects:
        bpy.data.objects.remove(obj)
    import_dmf(res)
    bpy.context.scene.tool_settings.lock_object_mode = False
    bpy.ops.wm.save_as_mainfile(filepath=r"C:\OTHER_PROJECTS\decima\test.blend")
