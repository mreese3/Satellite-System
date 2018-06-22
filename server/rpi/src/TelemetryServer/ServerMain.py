'''
Raspberry Pi Telemetry Server Main Server Class
'''

from TelemetryServer.Output import OutputManager
from TelemetryServer.Sensor import SensorManager
from .Packet import Packet, SequenceNumber, Flags
from time import sleep
from configparser import ConfigParser
import sys

class TelemetryServerException(Exception):
    def __init__(self, value):
        self.value = value

    def __str__(self):
        return repr(self.value)

class Server(object):
    '''
    The Main Server Class (outer class)
        -Info:
            The Server class is a wrapper for an inner __Server class to
            enforce a singleton design pattern.  The Server class preforms some
            basic type checking, but most of the actual work is done in the
            __Server class methods.  This class should be the only thing the
            main method needs to instantiate.

        -Methods:
            setup(self, config_file:str) - Takes a filename as an argument and
                loads it as its configuation. Preforms some basic type checking
                before passing config_file to __Server.setup().

            run(self) - Runs the main server loop. Calls __Server.run()

    '''
    class __Server(object):
        '''
        The Inner Server Class
            -Info:
                This is where most of the main loop work happens.  This is done
                to enforce the singleton design pattern.  This class contains
                the actual main loop for the server in __Server.run()

            -Methods:
                setup(self, config_file:str) - Take a file as an argument from
                    the outer Server.setup() method.  It uses the function
                    Config.getConfig(filefp:file) to parse the configuration,
                    stores the config items in instance memory and initializes
                    all the sensor and output devices based on the config.

                run(self) - This is the main server loop.  It is called by the
                    outer Server.run() method.
        '''

        def __init__(self):
            self.configuration_file = None
            self.sensor_list = list()
            self.output_device = None
            self.sleep_time = 0.0

        def setup(self, f):
            '''
            Configure The Server using the ini file in f.
            '''

            self.configuration_file = f.name

            creader = ConfigParser()
            creader.read_file(f)

            globaldict = dict()
            outputdict = dict()
            sensordict = dict()

            for section in creader.sections():
                # Catch Output Section
                if section == 'output':
                    for confitem in creader[section].keys():
                        outputdict[confitem] = creader[section][confitem]

                # Catch Global Section
                elif section == 'global':
                    for confitem in creader[section].keys():
                        globaldict[confitem] = creader[section][confitem]

                # All the other config is for sensors
                else:
                    sensordict[section] = dict()
                    for confitem in creader[section].keys():
                        # We Need to ensure that the SID is read as
                        # an integer, so we have to read this special
                        value = None
                        if confitem == 'sid':
                            value = creader[section].getint('sid')
                        else:
                            value = creader[section][confitem]

                        sensordict[section][confitem] = value

            # Now that we are done reading the config file,
            # time to implement the config
            f.close()

            # First configure the output
            self.output_device = OutputManager()
            self.output_device.setup(outputdict)

            # Next, configure the sensors
            for sensorname in sensordict.keys():
                sensor = SensorManager()
                sensor.setup(sensorname, sensordict[sensorname])
                self.sensor_list.append(sensor)

            # To Do: Any global Config items here
            for confitem in globaldict.keys():
                if confitem == 'sleeptime':
                    self.sleep_time = float(globaldict[confitem])

        def run(self):
            '''
            The main server loop.  This does all of the work for the server.

            No Return Value
            '''

            # Use packet SequenceNumber class to auto-increment sequence
            # numbers and wrap at 0xffffffff.
            seqno = SequenceNumber()

            # Our Packet Generator.  This packs our messages into packet
            # structs.
            packet_generator = Packet()

            # Determine the max payload that can be put into a packet.  This is
            # determined by the Output mtu and the packet overhead.  If the
            # output device has no mtu (mtu is set to 0), then set the
            # max_payload to the largest size possible (sys.maxsize seemes to
            # be a good large value to use)
            if self.output_device.getmtu() > 0:
                max_payload = (self.output_device.getmtu() -
                    packet_generator.getoverhead())
            else:
                max_payload = sys.maxsize

            # Simple Fair Queuing schedule for sending packets.  Each Sensor
            # has a queue associated with it.  The server gets a message from
            # this sensor and, if the message was larger than max_payload, it
            # puts an unfinished packet back.
            while True:
                # Query each of our sensors and see if any is ready to send a
                # message
                for sensor in self.sensor_list:
                    if sensor.query():
                        current_pseqno, message = sensor.getdata()
                        current_flags = Flags()
                        current_seqno = seqno.num

                        # If the message is larger than max payload, put the
                        # remainder of the message back into the sensor's
                        # queue.  It will send the rest of it next time the
                        # sensor is queried.
                        if len(message) > max_payload:
                            # Set the continue flag, so that the receiver knows
                            # that this will be a multi-part message
                            current_flags.f_continue = True

                            # put back the remainder of the message, with the
                            # current sequence number so the previous seqno can
                            # be set the next time this is sent
                            sensor.replacedata(current_seqno,
                                    message[max_payload:])
                            message = message[0:max_payload]

                        # If we encounter a message that is the end of a
                        # multi-part, set the end flag.
                        elif len(message) <= max_payload and current_pseqno > 0:
                            current_flags.f_end = True

                        # use the packet generator to packet the message into a
                        # packet for sending.
                        packet = packet_generator.pack(
                                current_flags.flags,
                                current_seqno,
                                current_pseqno,
                                sensor.getsid(),
                                message)

                        # Send the packet to the output queue to be sent off
                        self.output_device.queuemessage(packet)

                # sleep for some amount of time so the server main loop does
                # not take up 100% of a processor and counter-based sync
                # sensors work correctly (actually delay for some time)
                sleep(self.sleep_time)

    # Singleton wrapper class stuff
    instance = None

    def __init__(self):
        if not Server.instance:
            Server.instance = Server.__Server()
        else:
            raise TelemetryServerException('Server Already Initialized')

    def setup(self, config_file):
        '''
        Setup the server using the configuration file named in the config_file
        parameter

        No Return Value
        '''

        if Server.instance.configuration_file != None:
            return 1

        if not isinstance(config_file, str):
            raise TelemetryServerException('Argument not a filename!')

        else:
            try:
                f = open(config_file, 'r')
                Server.instance.setup(f)
            except FileNotFoundError as e:
                raise TelemetryServerException('Could Not Open ' \
                        'Configuration File: ' + e.strerror)


    def run(self):
        '''
        Run the server.

        No Return Value
        '''
        Server.instance.run()
