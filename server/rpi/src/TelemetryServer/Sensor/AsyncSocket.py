'''
Module for Example Asynchronous Sensor.  Implements a Socket Listener

Extra Configuration Items:
    -sockettype - Set the type of socket to open
        Values: 'SOCK_STREAM' (TCP) or 'SOCK_DGRAM' (UDP)
        Default: SOCK_STREAM

    -port - Set the port value for the socket
        Values: Integer between 1-65535
        Default: 10000

    -bindaddress - Set the address to bind to
        Values: IP address
        Default: '' (Any Address)
'''

from .AbstractAsyncSensor import *
from socket import *

class AsyncSocket(AbstractAsyncSensor):
    '''
    Example Async Sensor implementing a socket listener.
    '''

    def __init__(self):
        super().__init__()

    def worker(self):
        # our values
        sockettype = SOCK_STREAM
        port = 10000
        bindaddress = ''

        # check our config
        if 'sockettype' in self.config.keys():
            sockettype = eval(self.config['sockettype'])

        if 'port' in self.config.keys():
            port = eval(self.config['port'])

        if 'bindaddress' in self.config.keys():
            bindaddress = self.config['bindaddress']

        sock = socket(family=AF_INET, type=sockettype, proto=0)
        sock.bind((bindaddress, port))
        sock.listen(1)

        while True:
            clientsock, clientaddr = sock.accept()
            aggregatedbuffer = bytes()

            while True:
                tempbuffer = clientsock.recv(131072)
                if tempbuffer:
                    aggregatedbuffer += tempbuffer
                else:
                    break

            clientsock.close()
            self.queue.put(aggregatedbuffer)

def factory():
    '''
    Factory method
    '''
    return AsyncSocket()
