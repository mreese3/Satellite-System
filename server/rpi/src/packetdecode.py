#!/usr/bin/env python

# very simple packet decoder.  For testing.  Breaks quite easily!
from TelemetryServer import Packet
import sys

def printmessage(message):
    try:
        flags, seqno, pseqno, sid, length, isvalid, message = p.unpack(message)
    except:
        return
    myflags = Packet.Flags(flags)

    print("--------------------------------")
    print("Flags:\n\tContinue:\t" + str(myflags.f_continue) + \
            "\n\tEnd:\t\t" + str(myflags.f_end) + \
            "\n\tArmored:\t" + str(myflags.f_armored) + \
            "\n\tUrgent:\t\t" + str(myflags.f_urgent)
        )

    print("Seqno: %u\nPseqno: %u" % (seqno, pseqno))
    print("SID: %u" % sid)
    print("Message Length: %u" % length)
    print("Valid Message: " + str(isvalid))

p = Packet.Packet()
with open(sys.argv[1], 'rb') as f:
    while True:
        last_two = bytearray((0x00, 0x00))
        message = bytes()

        while last_two != b'\xde\xad':
            last_two[0] = last_two[1]
            last_two[1] = ord(f.read(1))

        message += bytes(last_two)

        while last_two != b'\xbe\xef':
            b = f.read(1)
            message += b
            last_two[0] = last_two[1]
            last_two[1] = ord(b)

        printmessage(message)

