import numba 
from numba.experimental import jitclass
from numba.core import types
from numba.extending import typeof_impl
from numba.extending import type_callable
from numba.extending import models, register_model
from numba.extending import make_attribute_wrapper
from numba.extending import lower_builtin
from numba.core import cgutils


spec = [
    ('fog_device_id', types.int64),               
    ('event_name', types.string),           
    ('event_type', types.int64),
    ('event_id', types.int64),
    ('timestamp', types.int64),
    ('availableMips', types.float64),
    ('taskId', types.string),
    ('dataSize', types.int64),
    ('mi', types.int64),
    ('maxMips', types.float64),
    ('consideredFogNodes', types.int64[:]),
    ('consideredField', types.float64[:,:]),
]


@jitclass(spec)
class Event:
    def __init__(self, fog_device_id, event_name, event_type, event_id, timestamp, availableMips, taskId, dataSize, mi, maxMips, consideredFogNodes, consideredField):

        self.fog_device_id = fog_device_id
        self.event_name = event_name
        self.event_type = event_type
        self.event_id = event_id
        self.timestamp = timestamp
        self.availableMips =availableMips
        self.taskId = taskId
        self.dataSize = dataSize
        self.mi = mi
        self.maxMips = maxMips
        self.consideredFogNodes = consideredFogNodes
        self.consideredField = consideredField


