'''
Module For Abstract Base Class for Synchronous Sensors  Using Delay Counter

Configuration Items:
    -counter - an integer to specify how many time query() should return false.
        This is to prevent the sensor from returning every time.  The actual
        time that the read is prevented depends on the server's timing.
        Example: if server loop delay is 5ms, then a counter of 1000 would
        result in a 5 second delay between reads.
        Values: A positive integer
        Default: Set by module or none
'''

from .AbstractSyncSensor import *

class AbstractSyncCounterSensor(AbstractSyncSensor):
    '''
    Abstract Bass Class For Synchronous Sensors Using Delay Counter
    '''

    def __init__(self):
        super().__init__()

        # see note in AbstractAsyncSensor for this
        self.config['async'] = False

        # Our Counter
        self.counter = 1

    def query(self):
        '''
        If there is a counter option set in the config, check if our counter is
        equal to it.  If not, increment the counter and return false.  If so,
        reset the counter to 1 and return true.  If there is no counter, always
        return true.  The counter delays getting the data from a sensor by
        returning false for x amount of times.  The actual time that getting
        data is delayed depends on the server loop's timing.

        Implementing classes should either set self.config['counter'] in
        postconfig() or be ready to be read from every server loop.

        Return Values:
            -True - The Sensor is ready to be queried (the counter has expired
                there was no counter set)
            -False - The Sensor is not ready (the counter has not expired)
        '''

        if 'counter' in self.config.keys():
            if self.counter == eval(self.config['counter']):
                self.counter = 1
                return True
            else:
                self.counter += 1
                return False
        else:
            return True

