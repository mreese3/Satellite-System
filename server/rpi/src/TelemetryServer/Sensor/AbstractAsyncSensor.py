'''
Module for Abstract Base Class for Asynchronous Sensors
'''

from .AbstractSensor import *
from multiprocessing import Process
from multiprocessing import Queue

class AbstractAsyncSensor(AbstractSensor):
    def __init__(self):
        super().__init__()
        self.proc = Process()   # Dummy Assignment
        self.queue = Queue()

        # this breaks the rules, but its just a flag
        # track if a sensor is async or not
        self.config['async'] = True

    def postconfig(self):
        '''
        postconfig for Async Sensors.  This should be called by child classes
        first in there postconfig().  Starts the worker process for the
        sensor.

        No Return Values.
        '''
        super().postconfig()
        self.proc = Process(name=self.config['name'], target=self.worker,
                daemon=True)

        self.proc.start()

    def worker(self):
        '''
        worker function launched as a separate process.  Not Implemented In
        AbstractAsyncSensor. Child classes should implement this method and
        use the queue to communicate back to the parent process.  The worker
        should be able to configure itself from the config dict, and then
        start an infinite loop so that it can run in the background.

        No Return Values.
        '''
        raise NotImplementedError('Method Not Implemented in' \
                ' Abstract Base Class')

    def query(self):
        '''
        Most asynchronous modules will just use the queue as the means to
        communicate between processes.  The Queue from mulitprocessing does
        locking on memory, so synchronization should not be an issue.
        By default, this function returns if the queue is empty or not.
        This can be overriden by child classes if needed.

        Return Values:
            -True - There is data in the queue to be read
            -False - The queue does not have any data to be read
        '''
        return not self.queue.empty()

    def getdata(self):
        '''
        Most asynchronous modules will just use the queue as the means to
        communicate between processes.  The Queue from mulitprocessing does
        locking on memory, so synchronization should not be an issue.
        By default, this function just returns the front of the queue.

        Return Values:
            -bytes - The data on the front of the queue
            -None - There was no data in the queue
        '''
        try:
            data = self.queue.get_nowait()
        except Queue.Empty:
            data = None

        return data
