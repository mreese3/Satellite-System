'''
Module for a Bluetooth Socket listener.  Opens a Bluetooth Socket and listens
for input.  Note: for this to work, the main server must be run as root.  I
have not found a way around this yet.  -Sean

Configuration Items:
    -socketproto - Set the Bluetooth socket protocol (L2 protocol?)
        Values: RFCOMM, L2CAP, HCI, SCO (Note: Only RFCOMM is implemented)
        Default: RFCOMM

    -localaddr - The address of the local Bluetooth interface to listen on
        Values: MAC Address
        Default: First interface found

    -port - The bluetooth port to listen on.
        Values: Integer
        Default: Any port

    -servicename - The Service Name used in advertisement
        Values: String
        Default: The Sensor Name

    (Other configuration options to come later)
'''

from .AbstractAsyncSensor import *
import bluetooth

class AsyncBluetoothSocket(AbstractAsyncSensor):
    '''
    Bluetooth Scoket Listener Class
    '''
    def __init__(self):
        super().__init__()

    def worker(self):
        '''
        Worker method for bluetooth socket

        No Return Value
        '''
        if 'socketproto' in self.config.keys():
            socketproto = eval('bluetooth.' + self.config['socketproto'])
        else:
            socketproto = bluetooth.RFCOMM


        if 'localaddr' in self.config.keys():
            localaddr = self.config['localaddr']
        else:
            localaddr = bluetooth.read_local_bdaddr()[0]

        if 'port' in self.config.keys():
            port = eval('bluetooth.' + self.config['port'])
        else:
            port = bluetooth.PORT_ANY

        if 'servicename' in self.config.keys():
            servicename = self.config['servicename']
        else:
            servicename = self.config['name']

        btsocket = bluetooth.BluetoothSocket(proto=socketproto)
        btsocket.bind((localaddr, port))

        btsocket.listen(1)

        bluetooth.advertise_service(btsocket, servicename,
                service_classes=[bluetooth.SERIAL_PORT_CLASS],
                profiles=[bluetooth.SERIAL_PORT_PROFILE])

        while True:
            clientsocket, clientaddress = btsocket.accept()

            data = bytes()
            while True:
                try:
                    data += clientsocket.recv(131072)
                except:
                    break

            clientsocket.close()

            self.queue.put(data)


def factory():
    '''
    Factory Method

    Returns BluetoothSocket
    '''
    return AsyncBluetoothSocket()
