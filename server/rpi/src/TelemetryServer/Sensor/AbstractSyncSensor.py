'''
Module For Abstract Base Class for Synchronous Sensors
'''

from .AbstractSensor import *

class AbstractSyncSensor(AbstractSensor):
    '''
    Abstract Bass Class For Synchronous Sensors
    '''

    def __init__(self):
        super().__init__()

        # see note in AbstractAsyncSensor for this
        self.config['async'] = False

    def query(self):
        '''
        Synchronous sensors are more likely to not need to be queried before
        pulling data from them.  By default, this method just returns true.
        Can be overriden by child classes if query needs to actually work.

        Return Value: Always True
        '''
        return True
