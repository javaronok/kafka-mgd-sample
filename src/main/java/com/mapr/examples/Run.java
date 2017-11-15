package com.mapr.examples;

import java.io.IOException;

/**
 * Pick whether we want to run as producer or consumer. This lets us
 * have a single executable as a build target.
 */
public class Run {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("Must have either 'producer' or 'consumer' as argument");
        }

        String[] applicationArgs = new String[args.length-1];
        System.arraycopy(args, 1, applicationArgs, 0, args.length-1);

        switch (args[0]) {
            case "producer":
                Producer.main(applicationArgs);
                break;
            case "consumer":
                Consumer.main(applicationArgs);
                break;
            default:
                throw new IllegalArgumentException("Don't know how to do " + args[0]);
        }
    }
}
