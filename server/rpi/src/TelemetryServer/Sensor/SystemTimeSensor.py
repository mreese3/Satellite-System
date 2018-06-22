'''
Module for System Times

Configuration Items:
    -counter - See AbstractSyncCounterSensor.py for details.
        Values: A positive integer
        Default: 1000
'''

from .AbstractSyncCounterSensor import *
from datetime import datetime, timedelta
import json

class SystemTimeSensor(AbstractSyncCounterSensor):
    '''
    System Time Sensor.  Returns the current system time in UTC.  The time is
    returned in ctime format.  Also returns system uptime.  Data is returned in
    JSON format.
    '''

    def __init__(self):
        super().__init__()

    def postconfig(self):
        '''
        If the counter is not set, default to 1000.
        '''

        if not 'counter' in self.config.keys():
            self.config['counter'] = '1000'

    def getdata(self):
        '''
        Returns the current time in UTC in ctime format with spaces replaced by
        underscores and uptime in json.  Example:
        {'system_time': 'Sat_Apr_23_00:30:00_2016', 'uptime': 00:1:00}

        Return Value:
            bytes - JSON data
        '''

        with open('/proc/uptime', 'r') as f:
            uptime = str(
                    timedelta(seconds=float(f.readline().split()[0]))
                    ).split('.')[0]

        data = {
                'system_time': datetime.utcnow().ctime().replace(' ', '_'),
                'uptime': uptime,
                }


        return json.dumps(data)

def factory():
    '''
    Return a SystemTimeSensor object
    '''
    return SystemTimeSensor()
