'''
Module for File Output
'''

from .AbstractOutput import *
from ..Packet import Packet

class FileOutput(AbstractOutput):
    '''
    File output.  Prints to file 'output.txt' by default
    '''
    def __init__(self):
        super().__init__()
        self.config['mtu'] = 0 # default to no mtu/unlimited packet size
        self.rpacket = Packet()

    def queuemessage(self, data):
        '''
        prints data to file and returns the length of data
        '''
        self.queue.put(data)

    def worker(self):
        '''
        Opens the file 'output.txt' and writes all output to that file.  Needs
        to flush after write, or sometimes the write fails for some reason
            - Sean

        No Return Value
        '''
        fp = open('output.txt', 'wb')
        while True:
            packet = self.queue.get()
            fp.write(packet)
            fp.flush()

def factory():
    '''
    Return a FileOutput Object
    '''
    return FileOutput()
