package edu.esu.syssoft.androidcontroller;

import java.math.BigInteger;


// Multiple loops in the codebase need a nice, simple way of
// of checking how long they've run. Android has many Timer, Handler, Stopwatch, etc. etc.
// classes. They are all basically system schedulers, and are massive overkill for me...

// NB: This is not an accurate Time class. If you need second precision, use getCurrentTime.
//	This is for calculating intervals, and handling wait timeouts.
//	It doesn't necessarily matter if pictured fires at 30 seconds or 40 seconds, etc.
//	It *does* matter that the time between calls is at least 30 seconds.

public class DumbTimer{
	// If BigInt overflows, the JVM has already died from OOM killer.
	// Also note, the BigInt, is immutable!
	private BigInteger currentTimeInSeconds;
	DumbTimer() {
		currentTimeInSeconds = BigInteger.ZERO;
	}

	public void tick() {
		currentTimeInSeconds = currentTimeInSeconds.add(BigInteger.ONE);
	}

	public void incrementSeconds(long s) throws ArithmeticException{
		if (s < 0) {
			throw new ArithmeticException("Received negative increment value.");
		}
		else {
			currentTimeInSeconds = currentTimeInSeconds.add(BigInteger.valueOf(s));
		}
	}

	// This method takes a long time and boolean. If the boolean is true, we don't increment,
	// but instead, reset the clock. This exists, because I am lazy, and I like to keep
	// conditionals at a minimum. Note that the caller still has to reset their boolean.
	public void incrementSecondsOrReset(long s, boolean reset) {
		if (reset) {
			reset();
		}
		else {
			incrementSeconds(s);
		}
	}

	public long getCurrentTime() {
		return currentTimeInSeconds.longValue();
	}

	public void reset() {
		currentTimeInSeconds = BigInteger.ZERO;
	}



}
