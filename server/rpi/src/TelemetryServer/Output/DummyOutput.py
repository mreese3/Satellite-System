'''
Module for Output Example
'''

from .AbstractOutput import *
from ..Packet import Packet

class DummyOutput(AbstractOutput):
    '''
    Example Output Device.  Prints to stdout.
    '''
    def __init__(self):
        super().__init__()
        self.config['mtu'] = 0 # default to no mtu/unlimited packet size
        self.rpacket = Packet()

    def queuemessage(self, data):
        '''
        prints data to stdout and returns the length of data
        '''
        self.queue.put(data)

    def worker(self):
        while True:
            packet = self.queue.get()
            print(packet)

def factory():
    return DummyOutput()
