'''
To Do

Configuration Items:
    -counter - See AbstractAsyncCounterSensor.py for details
        Values: A positive Integer
        Default: 1000
'''

from .AbstractSyncCounterSensor import *

class SystemTempSensor(AbstractSyncCounterSensor):
    '''
    System Temperature sensor.
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
        To Do

        Return Values:
            Bytes - The temperature in C
        '''
        with open('/sys/class/thermal/thermal_zone0/temp') as f:
            temp = str(float(f.readline()) / 1000)

        return bytes(temp, 'utf-8')

def factory():
    return SystemTempSensor()
