# SCMoo
A MOO written in sclang

Relies on BiLETools and JSONlib


## Loading and Saving

To start a new moo as a Root user:

    { m = Moo.bootstrap(NetAPI.broadcast("foo", "bar")); }.fork


To save your objects to a file:

    (
    f = File("/tmp/Moo.JSON".standardizePath, "w");
    f.write(m.toJSON);
    f.close;
    )


To load your saved objects

    (
    {
	  n = NetAPI.broadcast("foo", "bar");
	  5.wait;
	  m = Moo.bootstrap(n, "/tmp/Moo.JSON".standardizePath);
    }.fork;
  )


## Building Moo rooms and objects

You can build out the moo using a few commands from the window that opens.
To make a new object, type

	make objectName

If you want to use the object later in sclang, it's easiest if the name is lower case with no spaces. If you do want a space in the name, you must use double quotes:

	make "object name"

You can describe your object

	describe objectName as "A very nice object. The best object."

Then, you can look at it

	look objectName

You can drop it

	drop objectName


So for an example, we might:

	make cat
	describe cat as "A thoroughly fluffy beast. You've never seen so much hair. Thank goodness its a hypoallergenic breed!"
	drop cat


If you want to create a new room, you can do that by also naming an exit to that room ```exit exitName to newRoomName```

	exit north to pole

This will tell you the object ID number for both the room you're in and your new room. You can use these numbers to create your map

	Here (Lobby) is object number 2567
	New room pole is object number 3578
	New exit north to pole

So to make an exit back to the Lobby:

	north
    exit south to 2567

You can use these numbers elsewhere:

	exit portal to 3578


## The moo in sclang

Every object in the Moo, including your player and your location is a NetworkGui, which is an Environment.
If we've saved to moo in m (as in the very first example at the top of this file), we can use it in an sc document window.
In an example above, we made a cat and dropped it, so it's in our room. Let's do some stuff with the cat, in a document. A small window will open with a play button. Boot the server and press play to play it.

	m.me.location.push;
	~cat.pattern_(Pdef(\cat, Pbind(\degree, Pwhite(0,5))));
	~cat.show;
	~cat.property_(\freq);
	Pdef(\cat, Pbind(\freq, Pfunc({~cat.freq})));

You'll note that the labels in the window start with a number. That is the object's ID.
You can also use sclang to write verbs on your objects:

	~cat.verb_(\pet, \this, \none,
		{|dobj, iobj, caller, object|

			caller.location.announceExcluding(caller, "% pets %.".format(caller.name, object.name), caller);
			caller.postUser("You pet %".format(object.name), caller);
			1.rrand(3).wait;
			caller.location.announce("% purrs happily.".format(object.name), caller);
			1.wait;
			caller.location.announceExcluding(caller, "% angrily bites %.".format(object.name, caller.name), caller);
			caller.postUser("% angrily bites you.".format(object.name), caller);
		}
	);

This verb is called pet, it acts on the direct object and does not take an indirect object. When it's invoked,
the function is called with the direct object, the indirect object (nil in this case), the caller - the player
who is doing the invoking, and the cat.

	pet cat

In the above example, ```caller.location.announceExcluding(excluded, string, caller)``` sends a text output
message to everyone but the player listed as excluded. The message is string. And caller is the person
generating it.

```caller.postUser(string, caller)``` sends string to just the caller.

```caller.location.announce(string, caller)``` sends string to everyone in the room

### Changing properties

Verbs can also change properties:

	~cat.verb_(\pet, \this, \none,
		{|dobj, iobj, caller, object|

			caller.location.announceExcluding(caller, "% pets %.".format(caller.name, object.name), caller);
			caller.postUser("You pet %".format(object.name), caller);
			object.freq_(550, caller);
			1.rrand(3).wait;
			caller.location.announce("% purrs happily.".format(object.name), caller);
			object.freq_(440, caller);
			1.wait;
			caller.location.announceExcluding(caller, "% angrily bites %.".format(object.name, caller.name), caller);
			caller.postUser("% angrily bites you.".format(object.name), caller);
			object.freq_(880, caller);
		}
	);

Previously, we set a property on cat called freq. Here, we change that with ```object_freq(f, caller)``` where we
pass the new frequency and the user making the change.
These changes and changes to the sliders should propegate to all other users on your network.

Note that when you export your moo to a JSON file, this (should) also export all your properties and verbs.

Let's say your friend Bob loves your cat. You can make them a clone. In the moo interface type

	copy cat as felix

Then drop the cat where Bob can pick it up.

## To do

No give verb yet
Pdefs, patterns and synths are not yet shared, nor saved to JSON
