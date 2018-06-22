'''
Dummy Synchronous sensor - uses stdin
'''

from .AbstractSyncSensor import *

class SyncDummy(AbstractSyncSensor):
    def __init__(self):
        super().__init__()

    def getdata(self):
        '''
        Ask for user input and return the results
        '''
        return input('SyncDummy: ')

def factory():
    '''
    Note, all the sensor modules should implement a factory method.
    This gives a common name for SensorManager to call in every
    module.
    '''
    return SyncDummy()
