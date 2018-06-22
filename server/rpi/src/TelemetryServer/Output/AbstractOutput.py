'''
Module for Abstract Base Class for Output Devices
'''

from multiprocessing import Process
from multiprocessing import Queue

class AbstractOutput(object):
    '''
    Abstract Base Class for Output Devices
    '''

    def __init__(self):
        self.config = dict()
        self.proc = Process()
        self.queue = Queue()

    def queuemessage(self, data):
        '''
        Write data to output queue.  Not implemented here

        Return Values:
            Returns the amount of characters written
        '''
        raise NotImplementedError('Method Not Implemented in Abstract' \
                ' Base Class')

    def setconfig(self, key, value):
        '''
        Set configuration item inside the config dict.
        Inheriting classes should not have to override this
        and can use it as is.

        Return Values:
            0 - Item has been set
            1 - Item has been set, but already existed
        '''

        retvalue = 0
        if key in self.config.keys():
            retvalue = 1

        self.config[key] = value

        return retvalue

    def getconfig(self, key):
        '''
        Get config item from config dict.

        Return Values:
            Value - Could be any type that can be in dict
            None - The config item did not exist
        '''

        if key not in self.config.keys():
            return None

        else:
            return self.config[key]

    def postconfig(self):
        '''
        Call to implement configuration on the output device.  By default,
        starts the worker process.

        No Return Value
        '''

        if 'mtu' not in self.config.keys():
            raise KeyError('The Output Driver MTU is not set')

        self.proc = Process(name='Output', target=self.worker, daemon=True)
        self.proc.start()


    def worker(self):
        '''
        Asynchronous worker for output devices.  Launched by postconfig().
        This is run as a separate process and does the actual interaction with
        the output device.

        No Return Value
        '''
        raise NotImplementedError('Method Not Implemented in Abstract' \
                ' Base Class')
