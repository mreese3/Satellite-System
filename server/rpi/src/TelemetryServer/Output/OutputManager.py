
import importlib
from .AbstractOutput import *

class OutputManager(object):
    def __init__(self):
        self.output = AbstractOutput()

    def setup(self, outputdict):
        '''
        Loads and configures the output device base on dict passed as arg.
        Should preform all the configuration and then run the postconfig()
        method.

        No Return Value
        '''
        outputtype = outputdict.pop('type', 'AbstractOutput')
        module = importlib.import_module('.' + outputtype, 'TelemetryServer.Output')
        self.output = module.factory()
        if not isinstance(self.output, AbstractOutput):
            raise TypeError('Not an Output Class')

        for key, value in outputdict.items():
            self.output.setconfig(key, eval(value))

        self.output.postconfig()

    def queuemessage(self, data):
        '''
        Calls write from the output object.  Also checks if the data being
        written is in bytes format first.

        No Return Value
        '''
        if not isinstance(data, bytes):
            raise TypeError('Cannot write non-bytes object data')
        else:
            self.output.queuemessage(data)

    def getmtu(self):
        '''
        Returns mtu defined by output module

        Returns Values:
            Integer - the mtu of the output device
        '''
        return self.output.getconfig('mtu')
