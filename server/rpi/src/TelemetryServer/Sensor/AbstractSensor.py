'''
Module For Abstract Base Class for Sensor Classes
'''

class AbstractSensor(object):
    '''
    Abstract Bass Class For Sensor Modules
    '''

    def __init__(self):
        # Sensor Configuration dict
        self.config = dict()

    # Implemented Classes
    def setconfig(self, key, value):
        '''
        Set configuration item inside the config dict.
        Inheriting Classes should not have to override this
        and can use it as is.

        Return Values:
             0 - Item has been set
             1 - Item has been set, but already existed
            -1 - There has been some sort of error (should not happen)
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
            Value - Could be any type that can be in a dict
            None - The config item did not exist
        '''

        if key not in self.config.keys():
            return None
        else:
            return self.config[key]

    def postconfig(self):
        '''
        Call after configuration is complete.  This method is used to
        implement configuration changes and preform basic checks.
        Implementing classes should override this method, but should
        call this method (super().postconfig()) first thing.  The default
        behavior is to check if the name and sid values are set in the
        config dict.

        Note: If there is a configuration error, raise an exception

        No Return Values
        '''

        # check if name exists
        if 'name' not in self.config.keys():
            raise KeyError('Name is not set in ' + str(type(self)))

        # check if name is a string
        elif not isinstance(self.config['name'], str):
            raise ValueError('Name is not a string in ' +
                    str(self.config['name']))

        # check if sid exists
        if 'sid' not in self.config.keys():
            raise KeyError('SID is not set in ' + self.config['name'])

        # check if sid is an integer
        elif not isinstance(self.config['sid'], int):
            raise ValueError('SID is not an integer in ' + self.config['name'])

        # also check if sid is a 16-bit unsigned int
        elif self.config['sid'] > 65535 or self.config['sid'] < 0:
            raise ValueError('SID is not a 16-bit Unsigned integer in ' +
                    self.config['name'])


    # Unimplemented Methods
    def query(self):
        '''
        Used by child classes to check if there is data ready to be
        read.  Not Implemented In AbstractSensor.

        Return Values:
            -True - There is data ready to be read
            -False - There is no data ready to be read
        '''
        raise NotImplementedError('Method Not Implemented in Abstract' \
                ' Base Class')

    def getdata(self):
        '''
        Used by child classes to return data from a sensor.
        Not Implemented In AbstractSensor.

        Return Values:
            -bytes - Data from a sensor in a bytes object
            -None - There was no data to read
        '''
        raise NotImplementedError('Method Not Implemented in Abstract' \
                ' Base Class')


    # Properties
    @property
    def sid(self):
        '''
        Returns sid.  Note, sid needs to be set to get this
        '''
        return self.config['sid']

    @property
    def name(self):
        '''
        Returns name.  Note, name needs to be set to get this
        '''
        return self.config['name']
