#!/usr/bin/env python

# run telemetry server
import TelemetryServer

if __name__ == '__main__':
    s = TelemetryServer.Server()
    s.setup('telemetry.ini')
    s.run()
