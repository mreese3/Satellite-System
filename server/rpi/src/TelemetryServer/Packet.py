'''
Packet Module - Take data and convert it into packet form
'''

import struct
from sys import byteorder

PACKET_HEADER_FORMAT = '!HxBIIHH'
PACKET_HEADER_SIZE = struct.calcsize(PACKET_HEADER_FORMAT)
PACKET_FOOTER_FORMAT = '!BxH'
PACKET_FOOTER_SIZE = struct.calcsize(PACKET_FOOTER_FORMAT)
PACKET_PREAMBLE = 0xDEAD
PACKET_POSTAMBLE = 0xBEEF

class Crc8(object):
    '''
    Calculate CRC-8 for packet footer
    '''
    def __init__(self, poly=0x07):
        self.crctable = bytearray(256)
        self.poly = poly

        for i in range(0, 256):
            crc = i
            for j in range(0, 8):
                if crc & 0x80:
                    xorval = self.poly
                else:
                    xorval = 0
                crc = (crc << 1) ^ xorval
            crc = crc & 0xff
            self.crctable[i] = crc

    def calculate(self, message):
        '''
        Do the crc calculation.  Message can be either a bytes object or string
        object.

        Return Value:
            Integer - The calculated crc
        '''
        if not isinstance(message, bytes):
            message = bytes(message.encode('utf-8'))
        crc = 0
        for i in range(0, len(message)):
            crc = self.crctable[(crc & 0xff) ^ message[i]]

        return crc

    def check(self, crc, message):
        '''
        Check a crc against a message to see if they match.

        Return Values:
            True - crc matches
            False - crc does not match
        '''
        calculated_crc = self.calculate(message)
        if crc == calculated_crc:
            return True
        else:
            return False


class Packet(object):
    '''
    Packet class.  Converts values into packet bytes objects.
    '''

    def __init__(self):
        self.crc_calculator = Crc8()

    def pack(self, flags, seqno, pseqno, sid, payload):
        '''
        pack a payload into a packet bytes object
        '''
        packet = struct.pack(PACKET_HEADER_FORMAT,
                PACKET_PREAMBLE, flags, seqno, pseqno, sid, len(payload))
        if isinstance(payload, str):
            payload = bytes(payload.encode('ascii'))
        packet += payload
        packet_crc = self.crc_calculator.calculate(packet)
        packet += struct.pack(PACKET_FOOTER_FORMAT, packet_crc,
                PACKET_POSTAMBLE)
        return packet

    def unpack(self, packet):
        '''
        Unpacks a Packet into a tuple.

        Return Values:
            -Tuple - Packet Contents arranged in the following order:
                (flags, seqno, pseqno, sid, length, valid, payload)
        '''
        headertuple = struct.unpack(PACKET_HEADER_FORMAT,
                packet[0:PACKET_HEADER_SIZE])

        payload = packet[PACKET_HEADER_SIZE:PACKET_HEADER_SIZE+headertuple[5]]

        footertuple = struct.unpack(PACKET_FOOTER_FORMAT,
                packet[PACKET_HEADER_SIZE + headertuple[5]:])

        isvalid = self.crc_calculator.check(footertuple[0],
                packet[0:PACKET_HEADER_SIZE + headertuple[5]])

        return (
                headertuple[1],     # Flags Value
                headertuple[2],     # Sequence Number
                headertuple[3],     # Previous Sequence Number
                headertuple[4],     # SID
                headertuple[5],     # Payload Length
                isvalid,            # Boolean from CRC Check
                payload             # The payload message
                )

    def getoverhead(self):
        '''
        Returns the size of the header and footer.

        Return Values:
            -Integer - the size of the header and footer for a packet
        '''
        return PACKET_HEADER_SIZE + PACKET_FOOTER_SIZE

class SequenceNumber(object):
    '''
    Simple class to manage the sequence number.
    Automatically increments sequence number when used and deals with integer
    wrap around.
    '''

    def __init__(self):
        self.seq = 1

    def getseqnumber(self):
        '''
        Get the sequence number.
        '''
        if self.seq == 0xffffffff:
            self.seq = 2
        else:
            self.seq += 1

        # Return seq - 1 since we increment it before returning
        return self.seq - 1

    num = property(getseqnumber)

class Flags(object):
    '''
    Simple class to calculate flag values in the packet header.
    '''

    def __init__(self, unpack=None):
        '''
        Define a Flags object.  If specified, set default values to those in
        unpack.
        '''
        self.f_continue = False
        self.f_end = False
        self.f_armored = False
        self.f_urgent = False

        if unpack:
            if not isinstance(unpack, bytes) and not isinstance(unpack,
                    bytearray):
                raise TypeError("unpack must be a bytes-like object")
            value = int.from_bytes(unpack, byteorder)
            if value & (1 << 3):
                self.f_continue = True
            if value & (1 << 2):
                self.f_end = True
            if value & (1 << 1):
                self.f_armored = True
            if value & 1:
                self.f_urgent = True


    def getflags(self):
        '''
        Return flags integer value.
        '''
        val = 0x00
        if self.f_continue:
            val |= 1 << 3
        if self.f_end:
            val |= 1 << 2
        if self.f_armored:
            val |= 1 << 1
        if self.f_urgent:
            val |= 1

        return val

    flags = property(getflags)
