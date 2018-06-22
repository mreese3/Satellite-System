'''
SensorManager Class.  Abstracts Sensor work away from the server.
Allows for dynamic module loading so sensor types can be specified in the
configuration file.  Also implements the 'replace' function to make a returned
message appear at the front of the queue.
'''
import importlib
from .AbstractSensor import AbstractSensor

class SensorManager(object):
    '''
    Management Class to abstract away sensor configuration.
    '''

    def __init__(self):
        self.sensor = AbstractSensor()
        self.continue_buffer = None

    def setup(self, sensorname, sensordict):
        '''
        Loads and configures sensor based on dict passed as an argument.
        Should preform all the configuration and then run the sensor
        postconf() method.

        No Return Values
        '''

        # first, try to load the module for the sensor type
        sensortype = sensordict.pop('type', 'AbstractSensor')
        module = importlib.import_module('.' + sensortype,
            'TelemetryServer.Sensor')

        self.sensor = module.factory()
        if not isinstance(self.sensor, AbstractSensor):
            raise TypeError('Not a Sensor Class')

        # Now try to configure the sensor
        self.sensor.setconfig('name', sensorname)

        for key, value in sensordict.items():
            self.sensor.setconfig(key, value)

        # Finally, call postconfig()
        self.sensor.postconfig()

    def getsid(self):
        '''
        return sensor sid
        '''
        return self.sensor.sid

    def getname(self):
        '''
        return sensor name
        '''
        return self.sensor.name

    def query(self):
        '''
        return query results from sensor
        '''
        if self.continue_buffer != None:
            return True
        else:
            return self.sensor.query()

    def getdata(self):
        '''
        return data from the sensor.  If there is something in continue_buffer,
        it returns this.

        Return Values:
            -(0, bytes) - a new messge from the queue
            -(integer, bytes) - a continue_message
        '''
        returnvalue = None
        if self.continue_buffer != None:
            returnvalue = self.continue_buffer
            self.continue_buffer = None
        else:
            returnvalue = (0, self.sensor.getdata())
        return returnvalue

    def replacedata(self, pseqno, remaining):
        '''
        places an uncompleted message into local memory.
        This message has priority during the next getdata() request.

        No Return Value
        '''
        if self.continue_buffer == None:
            self.continue_buffer = (pseqno, remaining)
        else:
            raise BufferError('The continue buffer was already full in ' +
                    self.sensor.name)

    # Not Sure if these are necessary
    def setconfig(self, key, value):
        '''
        set config in sensor
        '''
        return self.sensor.setconfig(key, value)

    def getconfig(self, key):
        '''
        get config item from sensor
        '''
        return self.sensor.getconfig(key)

