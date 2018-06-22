'''
This module returns information about the cpu and memory untilization of the
local machine.  Packs the values in JSON.

Configuration Items:
    -counter - See AbstractAsyncCounterSensor.py for details
        Values: A positive Integer
        Default: 1000
'''

from .AbstractSyncCounterSensor import *
import psutil
import json

class SystemUtilSensor(AbstractSyncCounterSensor):
    '''
    System Utilization sensor.
    '''

    def __init__(self):
        super().__init__()

    def postconfig(self):
        '''
        If the counter has not been set, default to 1000

        No Return Values
        '''

        if not 'counter' in self.config.keys():
            self.config['counter'] = '1000'

    def getdata(self):
        '''
        Return the system utilization info.

        Return Values:
            JSON - JSON encoded utilization info
        '''
        values = {
                'cpu_percent': psutil.cpu_percent(),
                'mem_percent':  psutil.virtual_memory()[2],
                }

        return json.dumps(values)

def factory():
    return SystemUtilSensor()
