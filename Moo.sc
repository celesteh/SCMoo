Moo {
	classvar <>default;
	var index, objects, users, <api, <semaphore, <pronouns, <host, <>lobby, <me, <genericObject, <genericRoom, <genericPlayer;

	*new{|netAPI, json|
		^super.new.load(netAPI, json)
	}

	*login{|netAPI|
		^super.new.init_remote(netAPI)
	}

	*bootstrap {|api, foo|
		var doc, moo;

		//doc = Document();

		moo = Moo(api, foo);

		AppClock.sched(0, {
			doc = TextView.new(Window.new("", Rect(100, 100, 600, 700)).front, Rect(0, 0, 600, 700)).resize_(5);

			moo.me.me = true;
			moo.gui(doc);

			nil;
		});

		api.dump;

		^moo
	}




	init {|net|

		api = net ? NetAPI.default;

		api.isNil.if({
			Error("You must open a NetAPI first").throw;
		});

		semaphore = Semaphore(1);
		pronouns = IdentityDictionary[ \masc -> IdentityDictionary[
			\sub -> "He", \ob-> "Him", \pa-> "His", \po -> "His", \ref -> "Himself" ];
		];

		host = false;
	}


	load {|net, json|

		var root, user_update_action;

		Moo.default = this;

		this.init(net);

		api.dump;

		objects = [];
		index = 0;
		users = IdentityDictionary();

		host = true;

		json.isNil.if({

			"json is nil".postln;
			//moo.dump;
			//api.dump;
			//this.dump;

			//hack = this;

			//MooObject(this, "dummy");
			"make root".debug(this);
			root = MooRoot(this, "Jason");
			"made root".debug(this);
			genericObject = MooObject(this, "object", root, -1);
			"make a generic player".debug(this);
			genericPlayer = MooPlayer(this, "player", nil);
			"made generic player, %".format(genericPlayer.name).debug(this);
			genericPlayer.dump;
			root.parent = genericPlayer;

			genericRoom = MooRoom(this, "room", root, genericObject);
			genericRoom.description_("An unremarkable place.");
			lobby = MooRoom(this, "Lobby", root, genericRoom);
			lobby.arrive(root,lobby, root);

			me = root;

			//objects = [MooRoom(this, "Lobby", objects[0])];
			//index = 2;

		}, {
			// There needs to be a way to load this from JSON objects
		});

		//listen for new users
		user_update_action = {|buser|
			var name, muser;

			name = buser.nick.asSymbol;
			muser = users.at(name);
			muser.isNil.if({
				muser = MooPlayer(this, name);
			});
			lobby.arrive(muser);

		};
		api.add_user_update_listener(this, user_update_action );

	}

	init_remote{|net|
		var root;

		this.init(net);

	}


	add { |obj, name|

		var obj_index, should_add= true;

		semaphore.wait;

		obj.isKindOf(MooPlayer).if({
			name = name ? obj.name.asSymbol;
			users.includes(name).not({
				users.add(name);
			} , {
				// this user already exists
				obj_index = users.at(name).id;
				should_add = false;
				MooDuplicateError("MooPlayer % exists as %".format(name, obj_index)).throw;
			});
		});

		should_add.if({

			obj_index = objects.size;
			objects = objects.add(obj);
			index = index + 1;
		});

		semaphore.signal;

		^obj_index;
	}

	delete {| obj |

		var obj_index;

		obj.isInteger.if({
			obj_index = obj;
		} , {
			obj_index = objects.indexOf(obj);
		});

		obj_index.notNil.if({

			objects[obj_index] = nil; // Don't RENUM!!!!
		});
	}

	at {|ind|

		^objects.at(ind);
	}


	gui {|doc|

		var string;

		doc.isNil.if({
			//doc = TextView.new(Window.new("", Rect(100, 100, 600, 700)).front, Rect(0, 0, 600, 700)).resize_(5);
			Error("nil").throw;
		});

		doc.keyDownAction_({|doc, char, mod, unicode, keycode |
			var string;
			var returnVal = nil;
			var altArrow, altLeft, altPlus, altMinus;
			//[mod, keycode, unicode].postln;

			altArrow = Platform.case(
				\osx, { ((keycode==124)||(keycode==123)||(keycode==125)
					||(keycode==126)||(keycode==111)||(keycode==113)||
					(keycode==114)||(keycode==116)||(keycode==37)||
					(keycode==38)||(keycode==39)||(keycode==40))
				},
				\linux, {((keycode>=65361) && (keycode <=65364))},
				\windows, // I don't know, so this is a copy of the mac:
				{ ((keycode==124)||(keycode==123)||(keycode==125)
					||(keycode==126)||(keycode==111)||(keycode==113)||
					(keycode==114)||(keycode==116)||(keycode==37)||
					(keycode==38)||(keycode==39)||(keycode==40))
				}
			);

			altLeft = Platform.case(
				\osx, {((keycode==123) || (keycode==37))},
				\linux,{(keycode==65361)},
				\windows, // I don't know, so here's a copy of osx
				{((keycode==123) || (keycode==37))}
			);

			if( mod.isAlt && altArrow.value,
				{ // alt + left or up or right or down arrow keys
					"eval".postln;
					string = doc.selectedString;
					MooParser(me, string);

				}
			);

		});


		^doc;


	}

}




MooError : Error {}
MooVerbError : MooError {}
MooCompileError : MooVerbError {}
MooDuplicateError : MooError{}

MooReservedWordError : MooVerbError {
	var <>badStrings;
	*new {|msg, strings|
		^super.new.init(msg, strings);
	}
	init{|msg, strings|
		super.init(msg);
		badStrings = strings;
	}
}



MooObject : NetworkGui  {

	var <moo,  <owner, <id, <aliases, verbs, <location,  <properties, <>immobel,
	superObj;


	*new { |moo, name, maker|
		"MooObject.new".postln;
		^super.new.initMooObj(moo, name, maker);
	}

	initMooObj {|imoo, iname, maker, parent|

		var name, superID, superKey, public;


		moo = imoo ? Moo.default;

		moo.notNil.if({

			super.make_init(moo.api, nil, {});

			//moo = imoo;
			owner = maker;

			aliases = [];
			verbs = IdentityDictionary();
			properties = IdentityDictionary();
			immobel = false;


			//super.make_init(moo.api, nil, {});

			"about to do some parent stuff".debug(this);

			parent.notNil.if({
				parent.isKindOf(MooObject).if({
					superID = parent.id;
					superObj = parent;
				}, {
					superID = parent;
					superID.isKindOf(SimpleNumber).if({
						superObj = moo.at(superID);
					});
				});

				this.property_(\parent, superID, false, maker);
			});

			//playableEnv = NetworkGui.make(moo.api);
			//playableEnv.know = true;

			"about to copy properties".debug(this);
			this.pr_copyParentProperties();


			//playableEnv = NetworkGui.make(moo.api);
			//playableEnv.know = true;


			id = moo.add(this, iname);
			name = iname ? id.asInteger.asString;

			this.property_(\name, name, true, maker).value.postln;

			this.property(\description).isNil.if({
				this.property_(\description, "You see nothing special.", true, maker);
			});

			this.verb(\look).isNil.if({
				this.verb_(\look, \this, \none,
					"""
{|dobj, iobj, caller, object|
object.description.postln;
caller.post(object.description.value);
}
"""
				);
			});

			this.verb(\describe).isNil.if({
				this.verb_(\describe, \this, \any,
					"""
{|dobj, iobj, caller, object|

(caller == object.owner).if({
object.description.value_(iobj.asString);
});
}
"""
				);
			});

			this.verb(\drop).isNil.if({
				this.verb_(\drop, \this, \none,
					"""
{|dobj, iobj, caller, object|

caller.contents.remove(dobj);
caller.location.announce(\"% dropped %\".format(caller, dobj));
caller.location.contents = caller.location.contents.add(dobj);
}
"""
				);
			});
		});
	}


	formatKey{|key|

		^"%/%".format(key, id).asSymbol;
	}

	pr_copyParentProperties{

		var public;


		superObj.notNil.if({
			// copy the parent's properties
			superObj.properties.keys.do({|key|
				this.properties[key].isNil.if({
					// is it public?
					public = superObj.isPublic(key);
					this.property_(key, superObj.property(key).value, public);
				});
			});
		});


	}


	alias {|new_alias|

		aliases = aliases.add(new_alias);
	}


	location_{|loc|

		location = loc;
		"location %".format(location).debug(this.id);
	}

	isPublic{|key|

		var publicKey = this.formatKey(key);
		^ shared.includesKey(publicKey);
	}


	property_ {|key, ival, publish = true, changer|

		var shared;

		key = key.asSymbol;

		"property_ % %".format(key, ival).postln;

		verbs.includesKey(key).if({
			MooError("% name already in use by %".format(key, this.name)).throw;
		});

		properties.includesKey(key).if({
			// overwrite. Send notification
			properties.at(key).value_(ival, changer);
		}, {

			//shared = SharedResource(ival);
			//"shared %".format(shared.value).postln;
			//properties.put(key, shared);
			//publish.if({
			//	playableEnv.addShared("%/%".format(key, id).asSymbol, shared);
			//});
			publish.if({
				shared = this.addShared(this.formatKey(key), ival);
			} , {
				shared = this.addLocal(this.formatKey(key), ival);
			});
			properties.put(key, shared);
		});

		properties.keys.postln;
		"saved as %".format(properties[key].value).postln;

		^shared;
	}

	property {|key|
		var value;

		key = key.asSymbol;

		value = properties.at(key);

		value.isNil.if({
			superObj.notNil.if({
				value = superObj.property(key);
			});
		});

		^value
	}


	verb_ {|key, dobj, iobj, func, publish=false|

		var newV;

		"New verb %".format(key).postln;

		key = key.asSymbol;

		((properties.includesKey(key)) || verbs.includesKey(key)).if({
			MooError("% name already in use by %".format(key, this.name)).throw;
		}, {
			newV = MooVerb(key, dobj, iobj, func, this, publish);

			verbs.put(key, newV);
		});
	}

	getVerb {|key|

		var verb = verbs.at(key.asSymbol);


		verb.isNil.if({
			superObj.notNil.if({
				verb = superObj.getVerb(key);
			})
		});

		^verb;
	}

	verb {|key|
		^this.getVerb(key)
	}

	isPlayer{ ^false }


	doesNotUnderstand { arg selector ... args;
		var verb, property, func;

		selector = selector.asSymbol;


		// first try moo stuff
		//"..\ndoesNotUnderstand %".format(selector).debug(this.id);
		//verbs.keys.postln;
		//properties.keys.postln;
		//"..".postln;

		verb = verbs[selector];
		if (verb.notNil) {
			"verb %".format(selector).postln;
			//^func.functionPerformList(\value, this, args);
			^verb.invoke(args[0], args[1], args[2]);
		};

		"not a verb".postln;
		verbs.keys.postln;

		if (selector.isSetter) {
			selector = selector.asGetter;
			if(this.respondsTo(selector), {
				warn(selector.asCompileString
					+ "exists as a method name, so you can't use it as a pseudo-method.");
				this.dumpBackTrace;
				{
					this.perform(selector.asSetter, *args);
					"new result is %".format(this.perform(selector)).debug(this.id);
				}.try({|err| err.postln; });
			}, {
				"setter".debug(this.id);
				property = this.property(selector);
				property.notNil.if({
				    //^(properties[selector].value_(*args));
					// does this belong to us?
					properties[selector].notNil.if({
					   ^(properties[selector].value_(*args));
					} , {
					    // it must belong to a parent
						// we need a way of seeing if it's shared or not
						^this.property_(selector, *args);
					});
				});
			});
		};

		property = this.property(selector);//properties[selector];
		property.notNil.if({
			"porperty % %".format(selector, property.value).postln;
			^property.value;
		});

		"not a property".postln;
		properties.keys.postln;

		^nil;

	}


	move {|newLocation|

		var oldLocation, moved = false;

		immobel.not.if({
			this.isPlayer.not.if({
				oldLocation.remove(this);
				newLocation.addObject(this);

			} , {
				//name = this.property(\name);
				//oldLocation.player.remove(this);
				oldLocation.depart(this, oldLocation, this);
				newLocation.arrive(this, newLocation, this);
			});

			this.location = newLocation;
		})
	}


	matches{|key|

		var matches = false;

		matches = (key == this.name);

		matches.not.if({

			matches = aliases.includes(key)
		});

		^matches;
	}


	== {|other|

		other.isKindOf(MooObject).if({
			^(other.id == this.id);
		});

		false;
	}

	toJSON{

		var encoder, props;

		encoder = MooCustomEncoder();

		^"{ % }".format(this.pr_JSONContents(encoder));


	}

	pr_JSONContents {|encoder|

		var props = properties.collect({|p| JSONConverter.convertToJSON(p, encoder) });

		^"\"id\": %, ".format(id.asCompileString) +
		"\"verbs\": %,".format(JSONConverter.convertToJSON(verbs)) +
		"\"properties\": [ % ],".format(JSONConverter.convertToJSON(props)) +
		"\"aliases\": %,".format(JSONConverter.convertToJSON(aliases)) +
		"\"location\": %,".format(location !? { location.id } ? "null") +
		"\"immobel\": %,".format(immobel.asCompileString) +
		"\"owner\": % ".format(owner !? {owner.id} ? "null")
	}
}





/*
MooMusicalObject : MooObject {

var playableEnv;

*new { |moo, name, maker|

^super.new.init(moo, name, maker);
}

init {| moo, name, maker|

super.init(moo, name, maker);

playableEnv = NetworkGui.make(moo.api, nil, {});
playableEnv.know = true;

}

}
*/


MooClock : MooObject {

	var <stage, <clock;

	*new { |moo, name, maker, stage|
		^super.new.initClock(moo, name, maker, stage);
	}

	initClock {| moo, name, maker, istage|

		var sharedTempo;

		super.initMooObj(moo, name, maker);

		stage = istage;

		istage.notNil.if({
			stage = istage;
			location = stage.location;
			location.add(this);
		});


		clock = TempoClock();

		verbs = IdentityDictionary();


		// share tempo
		sharedTempo = this.property_(\tempo, 1).changeFunc_({|old, tempo|
			var changed = (old != tempo);
			clock.tempo = tempo;
			^changed;
		}).spec(\tempo, 1);

		this.verb_(\set, \this, \any,
			"""
{|dobj, iobj, caller|

sharedTempo.value = iobj.asFloat;
}
"""
		); //uses a property so doesn't need to be published

		this.verb_(\play, \this, \any,
			"""
{|dobj, iobj, caller|

clock.play
}
"""
			, true);

		this.verb_(\stop, \this, \any,
			"""
{|dobj, iob, caller|

clock.stop
}
"""
			, true);

		//desc = "The clock attached to %".format(stage.name);
		stage.notNil.if({
			this.description = "The clock attached to %".format(stage.name);
		} , {
			this.description = "A clock";
		});

	}


}

MooStage : MooObject {

	var players, clock, speakers;

	*new { |moo, name, maker|
		^super.new.initStagre(moo, name, maker);
	}

	initStage {| moo, name, maker|

		super.initMooObj(moo, name, maker);
		players = [];
		speakers = [];
		location = maker.location;
		immobel = true;

		clock = MooClock(moo, "%_clock".format(name), maker);

		verb_(\add, \any, \this,

			"""
{|dobj, iobj, caller|

(caller == owner).if({
dobj.isKindOf(MooPlayer).if({
players.includes(dobj).not ({
players = players.add(dobj);
})
})
})
}
"""
		);

	}
}



MooRoom : MooObject {

	var contents, players, exits, semaphore;

	*new { |moo, name, maker|

		^super.new.initRoom(moo, name, maker);
	}

	initRoom {| moo, name, maker|

		super.initMooObj(moo, name, maker);
		semaphore = Semaphore(1);
		players = [];
		contents = [];
		aliases = ["here"];
		exits = IdentityDictionary();

		this.verb(\announce).isNil.if({

		this.verb_(\announce, \any, \this,
			// announce "blah" to here
			"""
{|dobj, iobj, caller|

iobj.announce(dobj, caller);
}
"""
		);
		});

		this.verb(\arrive).isNil.if({
		this.verb_(\arrive, \any, \this,
			"""
{|dobj, iobj, caller|
\"arrive\".postln;
caller.isPlayer.if({

iobj.announce(\"With a dramatic flourish, % enters\".format(caller.name));
//players = players.add(caller);
caller.dumpStack;
iobj.addPlayer(caller);
caller.location = iobj;
});
}
"""
		);
		});

		this.verb(\depart).isNil.if({
		this.verb_(\depart, \any, \this,
			"""
{|dobj, iobj, caller|
caller.isPlayer.if({

//players.remove(caller);
iobj.removePlayer(caller);
iobj.announce(\"With a dramatic flounce, % departs\".format(caller.name));

});
}
"""
		);
		});
	}

	announce {|str, caller|

		var tell;

		players.do({|player|
			player.post(str, caller);
		});

		contents.do ({|thing|
			thing.verbs.includesKey(\tell).if({
				tell = thing.verbs.at(\tell);
				tell.invoke(thing, str, caller);
			});
		});
	}

	announceExcluding{|excluded, str|
		var tell;

		players.do({|player|
			(player != excluded).if({
				player.post(str);
			});
		});

		contents.do ({|thing|
			thing.verbs.includesKey(\tell).if({
				tell = thing.verbs.at(\tell);
				tell.invoke(thing, str);
			});
		});


	}


	remove {|item|

		semaphore.wait;
		contents.remove(item);
		//playableEnv.remove(item);
		this.remove(item);
		semaphore.signal

	}

	addObject {|item|

		semaphore.wait;
		contents = contents.add(item);
		//playableEnv.put(item.name.asSymbol, item);
		this.put(item.name.asSymbol, item);
		semaphore.signal

	}

	removePlayer {|player|
		semaphore.wait;
		players.remove(player);
		//playableEnv.remove(player);
		this.remove(player);
		semaphore.signal
	}

	addPlayer{|player|
		semaphore.wait;
		players = players.add(player);
		//playableEnv.put(player.name.asSymbol, player);
		this.put(player.name.asSymbol, player);
		semaphore.signal
	}

	exit{|key|
		^exits[\key]
	}

	addExit{|key, room|

		exits.put(key, room);
	}

	findObj {|key|

		var found, search;
		key = key.asSymbol;

		search = {|arr|

			arr.do({|obj|


				obj.matches(key).if({

					found = obj;
				})
			});

			found;
		};

		found = search.(contents);
		found.isNil.if({
			found = search.(players);
		});

		^found;
	}

	env {

		//^playableEnv
	}


	doesNotUnderstand { arg selector ... args;
		var found;

		selector = selector.asSymbol;

		found = super.doesNotUnderstand(selector, *args);

		found.isNil.if({
			found = this.findObj(selector);
			found.isNil.if({
				found = this[selector];//playableEnv[selector];
			});
		});

		^found;
	}

	remoteDesc { arg user;


	}

	pr_JSONContents {|encoder|

		/*
		var props = properties.collect({|p| JSONConverter.convertToJSON(p, encoder) });

		^"\"id\": %, ".format(id.asCompileString) +
		"\"verbs\": %,".format(JSONConverter.convertToJSON(verbs)) +
		"\"properties\": [ % ],".format(JSONConverter.convertToJSON(props)) +
		"\"aliases\": %,".format(JSONConverter.convertToJSON(aliases)) +
		"\"location\": %,".format(location !? { location.id } ? "null") +
		"\"immobel\": %,".format(immobel.asCompileString) +
		"\"owner\": % ".format(owner !? {owner.id} ? "null")
		*/

		var stuff, departures;

		stuff = contents.collect({|c| c !? { c.id } ? "null" });
		departures = exits.keysValuesDo({|key, val| "{ \"key\": \"%\", \"val\": %}".format(key, val !? {val.id} ? "null") });

		^super.pr_JSONContents(encoder) +
		"\"contents\": [ % ]," .format(stuff.join(", ")) +
		"\"exits\": [ % ]".format(departures.join(",\n"));
	}



	/*
	toJSON{

		var encoder, props, stuff, departures;

		encoder = MooCustomEncoder();

		props = properties.collect({|p| JSONConverter.convertToJSON(p, encoder) });
		stuff = contents.collect({|c| c !? { c.id } ? "null" });
		departures = exits.keysValuesDo({|key, val| "{ \"key\": \"%\", \"val\": %}".format(key, val !? {val.id} ? "null") });

		^ "{ \"id\": %, ".format(id.asCompileString) +
		"\"verbs\": %,".format(JSONConverter.convertToJSON(verbs)) +
		"\"properties\": [ % ],".format(props.join(",\n")) +
		"\"aliases\": %,".format(JSONConverter.convertToJSON(aliases)) +
		"\"location\": %,".format(location !? { location.id } ? "null") +
		"\"immobel\": %,".format(immobel.asCompileString) +
		"\"contents\": [ % ]," .format(stuff.join(", ")) +
		"\"exits\": [ % ]".format(departures.join(",\n")) +
		"\"owner\": % }".format(owner !? {owner.id} ? "null")
	}
	*/
}
