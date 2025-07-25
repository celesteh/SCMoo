MooI18n {

	classvar <>i18n, <>parser;

	*initClass{

		i18n = IdentityDictionary();
	}

	*new{|parser|
		^super.new.init(parser);
	}

	init {|parser|
		this.class.parser = parser;
	}

	*addPair{|key, value|

		value.isKindOf(Function).not.if({

			i18n[key] = value;
			// if we're looking up symbols, make it bidirectional
			value.isKindOf(Symbol).if({
				i18n[value] = key;
			})
		});
	}

	addPair{|key, value|
			this.class.addPair(key, value);
	}

	at{|key|
		^this.class.i18n.at(key);
	}

	parser {
		^this.class.parser;
	}

	addFormattedMsg {|key, str, orderMapping|
		this.addPair(key, [str, orderMapping]);
	}


	// we don't want to store functions, so we will store the order of array items and the string with %s
	*format {|key, args|
		// for an example, \newRoomMsg has
		// [locationName, locationID, newName, newID]
		// "Here (%) is object number %\nNew room % is object number %"
		// So in english, the key is \newRoomMsg and args is the array above
		// the key returns an array which is [the string, [0,1,2,3]]
		var str, arr;

		arr = this.at(key.asSymbol);
		str = arr[0];
		arr = arr.collect({|i| args.at(i) });

		^str.format(*arr);
	}

	format {|key ...args|
		^this.class.format(key, args);
	}

}



MooEn : MooI18n {

	*new {

		^super.new.init();
	}

	init {

		super.init(MooEnParser);

		this.addPair(\make, \make);
		this.addPair(\copy, \copy);
		this.addPair(\room, \room);
		this.addPair(\stage, \stage);
		this.addPair(\clock, \clock);
		this.addPair(\object, \object);
		this.addPair(\container, \container);
		this.addPair(\bag, \bag);
		this.addPair(\player, \player);
		this.addPair(\Lobby, \Lobby);
		this.addPair(\exit, \exit);
		this.addPair(\say, \say);
		this.addPair(\pose, \pose);
		this.addPair(\here, \here);
		this.addPair(\me, \me);

		this.addPair(\openString, $");
		this.addPair(\closeString,$");

		//this.addPair(\newRoomMsg, {|locationName, locationID, newName, newID|
		//	"Here (%) is object number %\nNew room % is object number %".format(locationName, locationID, newName, newID)
		//});
		this.addFormattedMsg(\newRoomMsg, "Here (%) is object number %\nNew room % is object number %",
			/* [locationName, locationID, newName, newID], */ [0,1,2,3]);


		//this.addPair(\newObjMsg, {|obj| "Made %.".format(obj) });
		this.addFormattedMsg(\newObjMsg, "Made %.", [0]);
		//this.addPair(\exitMsg, {|exitName, roomName| "New exit % to %".format(exitName, roomName) });
		this.addFormattedMsg(\exitMsg,  "New exit % to %", [0, 1]);
		//this.addPair(\inputErrorMsg, {|badString| "Input not understood." });
		this.addFormattedMsg(\inputErrorMsg, "Input not understood.", []);


		this.addPair(\defaultDesc, "You see nothing special.");
		this.addPair(\defaultRoomDesc, "An unremarkable place.");

	}


	updateGenericObject{|moo|

			moo.generics[\object].verb_(\get, \this, \none,
				{|dobj, iobj, caller, object|

					//"get".debug(object);

					object.immobel.not.if({

						object.location.remove(object);
						caller.addObject(object);
						caller.location.announceExcluding(caller, "% picked up %.".format(caller.name, object.name), caller);
						caller.postUser("You picked up %".format(object.name), caller);
					} , {
						caller.postUser("You cannot pick up %".format(object.name), caller);
					});
				}
			);

		/*

			moo.generics[\object].verb_(\verbs, \this, \none,
				{|dobj, iobj, caller, object|

					var ancestry, keys = object.verbs.keys.asList;

					// get verbs of parent objects
					ancestry = MooObject.mooObject(object.property(\parent).value, object.moo);
					{ ancestry.isKindOf(MooObject) }.while ({
						keys = keys.union(ancestry.verbs.keys.asList);
						//keys.debug(object.id);
						ancestry = MooObject.mooObject(ancestry.property(\parent).value, object.moo);
					});
				// exclude keys that start with @
				keys = keys.reject({|key| key.asString.beginsWith("@") });

					(keys.size == 0).if({
						caller.postUser("% has no verbs.".format(object.name), caller);
					}, {
						(keys.size == 1).if({
							caller.postUser("% has a verb: %.".format(object.name, keys.first), caller);
						}, {
							// more than 1
							caller.postUser("% has the verbs: %, and %."
								.format(object.name, keys.copyRange(0, keys.size-2).join(", "), keys.last), caller);
						})
					})
				}
			);

		*/

			moo.generics[\object].verb_(\look, \this, \none,

				{|dobj, iobj, caller, object|
					//object.description.postln;
				caller.postUser(object.description.value + "\n", caller);
				}.asCompileString;

			);


		moo.generics[\object].verb_('@describe', \this, \any,

				{|dobj, iobj, caller, object|

				//"describe".debug(object.name);

				((caller == object.owner) || caller.wizard).if({
					//"can describe".debug(object.name);
					//object.description.value_(iobj.asString);
					object.property_(\description, iobj.asString.stripEnclosingQuotes, false, caller);
					caller.postUser("You describe % as \"%\".".format(object.name, object.description.value), caller);
				}, {
					caller.postUser("You are not allowed to describe % because you are not the owner.".format(object.name), caller);
				});
				}.asCompileString;

			);


		moo.generics[\object].verb_(\drop, \this, \none,

			{|dobj, iobj, caller, object|

				//caller.contents.remove(dobj);
				caller.remove(dobj);
				caller.location.announce("% dropped %".format(caller.name, dobj.name), caller);
				//caller.location.contents = caller.location.contents.add(dobj);
				//caller.location.addObject(dobj);
				object.move(caller.location, caller);
			}.asCompileString;

		);

		moo.generics[\object].verb_('@examine', \this, \none,
			{|dobj, iobj, caller, object|

				var ancestry, keys = object.verbs.keys.asList;


				caller.postUser("% ID %\nAliases %\nProperties %".format(object.name, object.id,
					object.aliases, object.properties.keys.asArray), caller);
				caller.postUser(object.description.value + "\n", caller);

				// get verbs of parent objects
				ancestry = MooObject.mooObject(object.property(\parent).value, object.moo);
				{ ancestry.isKindOf(MooObject) }.while ({
					keys = keys.union(ancestry.verbs.keys.asList);
					//keys.debug(object.id);
					ancestry = MooObject.mooObject(ancestry.property(\parent).value, object.moo);
				});

				(keys.size == 0).if({
					caller.postUser("No obviousverbs.\n".format(object.name), caller);
				}, {
					caller.postUser("Obvious verbs are:\n%".format(keys.join("\n")), caller);
				});

			}.asCompileString;
		);

	}

	updateGenericContainer{|moo|

		//this.updateGenericObject(moo);

			moo.generics[\container].verb_(\inventory, \this, \none,

				{|dobj, iobj, caller, object|
					var str, last;
					//object.description.postln;
					(object.contents.size == 0).if({
						str = "% is empty.".format(object.name);
					}, {
						(object.contents.size == 1).if({
							str = "% contains %.".format(object.name, object.contents[0].name);
						}, {
							(object.contents.size > 1).if({
								last = object.contents.last;
								str =  "% contains % and %.".format(object.name,
									object.contents.copyRange(0, object.contents.size-2)
									.collect({|c| c.name }).asList.join(", "),
									last.name);
							})
						})
					});
					str.notNil.if({
						//str.debug(object);
						caller.postUser(str, caller);
					} , {
						"Should not be nil".warn;
					});
				}.asCompileString;

			);

		moo.generics[\container].verb_(\put, \any, \this,

			{|dobj, iobj, caller, object|

				var name, success = false;

				dobj.isKindOf(MooObject).if({

					name = dobj.name;

					success = object.addObject(dobj, caller); // put the dobj in the container
					// this returns false if we can't move the object

				} , {
					// dobj is a string or some such
					// We say it's going into the container for fun
					name = dobj;
					success = true;
				});

				success.if({
					caller.location.announceExcluding(caller, "% put % into %.".format
						(caller.name, name, object.name), caller);
					caller.postUser("You put % into %.".format(name, object.name), caller);
				} , {
					caller.postUser("You can't pick up %".format(name), caller);
				});

			}.asCompileString;

		);

		moo.generics[\container].verb_(\remove, \any, \this,
			{|dobj, iobj, caller, object|

				var item, name, success = true;

				dobj.isKindOf(MooObject).if({
					success = false; // see if it works
					name = dobj.name;
				});
				//	name.debug(object);
				item = object.remove(dobj, caller);
				item.notNil.if({
					success = caller.addObject(item, caller);
					//dobj.location= caller;
				});
				item.isKindOf(MooObject).if({
					name = item.name;
				} , {
					name = name ? dobj;
				});

				success.if({
					caller.location.announceExcluding(caller, "% removed % from %.".format
						(caller.name, name, object.name), caller);
					caller.postUser("You removed % from %.".format(name, object.name), caller);
				} , {
					caller.postUser("Try as you might, you can't remove % from %".format(name, object.name), caller);
				});



			}.asCompileString;

		);




	}

	updateGenericRoom{|moo|

		//this.updateGenericContainer(moo);

			moo.generics[\room].verb_(\announce, \any, \this,
				// announce "blah" to here

				{|dobj, iobj, caller, object|

					object.announce(dobj, caller);
				}.asCompileString;

			);


			moo.generics[\room].verb_(\arrive, \any, \this,

			{|dobj, iobj, caller, object|
				"arrive".debug(object.name);
				caller.isPlayer.if({
					//"caller. is a player".debug(object.name);
						object.announceExcluding(caller, "With a dramatic flourish, % enters".format(caller.name), caller);
						//players = players.add(caller);
						//caller.dumpStack;
						object.addPlayer(caller);
					caller.location_(iobj, caller.name);
						object.getVerb(\look).invoke(object, object, caller, object);
					});

				}.asCompileString;

			);


			moo.generics[\room].verb_(\depart, \any, \this,

				{|dobj, iobj, caller, object|
					caller.isPlayer.if({

						//players.remove(caller);
						object.removePlayer(caller);
						object.announceExcluding(caller, "With a dramatic flounce, % departs".format(caller.name), caller);

					});
				}.asCompileString;

			);


		moo.generics[\room].verb_(\look, \this, \none,

			{|dobj, iobj, caller, object|
				var stuff, others, last, exits;
				//object.description.postln;
				caller.postUser(object.name.asString +"\n" + object.description.value, caller);
				(object == caller.location).if({
					stuff = object.contents;
					//"stuff".debug(object);
					(stuff.size > 0).if({
						stuff = stuff.collect({|o| MooObject.mooObject(o, object.moo).name });
						stuff = stuff.join(", ");
						caller.postUser("You see:" + stuff, caller);
					});
					others = object.mooPlayers.select({|player| player != caller });
					(others.size == 1).if({
						caller.postUser(MooObject.mooObject(others[0], object.moo).name.asString + "is here.", caller);
					}, {
						(others.size > 1).if({
							last = MooObject.mooObject(others.pop, object.moo);
							others = others.collect({|o|  MooObject.mooObject(o, object.moo).name  });
							others = others.join(", ");
							caller.postUser(others ++", and" + last.name + "are here.", caller);
						});
					});
					exits = object.exits.keys.asList;
					(exits.size == 1).if({
						//"one exit %".format(exits.first).debug(object);
						caller.postUser("You can exit" + exits[0].asString, caller);
					}, {
						(exits.size > 1).if({
							caller.postUser("Exits are:" + exits.join(", "), caller);
						}, {
							(exits.size==0).if({
								caller.postUser("There is no way out.", caller);
							});
						});
					});
				});
				caller.postUser("", caller);
			}.asCompileString;

		);

		moo.generics[\room].verb_(\inventory, \this, \none,

			{|dobj, iobj, caller, object|
				var str, last, skip = false, callerVerb;

				// Did the caller specify the room?
				dobj.isNil.if({
					// no, this was called by default
					(caller.location == object).if({
						// and the caller is in the room

						callerVerb = caller.verb(\inventory);
						callerVerb.notNil.if({
							skip = true;
							callerVerb.invoke(dobj, iobj, caller, caller);
						});
					});
				});

				// No, the user wants to see our contents
				skip.not.if({
					//object.description.postln;
					(object.contents.size == 0).if({
						str = "% is empty.".format(object.name);
					}, {
						(object.contents.size == 1).if({
							str = "% contains %.".format(object.name, object.contents[0].name);
						}, {
							(object.contents.size > 1).if({
								last = object.contents.last;
								str =  "% contains % and %.".format(object.name,
									object.contents.copyRange(0, object.contents.size-2)
									.collect({|c| c.name}).asList.join(", "),
									last.name);
							})
						})
					});
					str.notNil.if({
						//str.debug(object);
						caller.postUser(str, caller);
					} , {
						"Should not be nil".warn;
					});
				});
			}.asCompileString;

		);

	}

	updateGenericPlayer{|moo|



		//no testing if they're already set. These methods shouldn't be changed except by an end user
		moo.generics[\player].verb_(\tell, \this, \any,
			{|dobj, iobj, caller, object|
				object.postUser(iobj.asString, caller);
			}.asCompileString;
		);

		moo.generics[\player].verb_(\say, \this, \any,
			{|dobj, iobj, caller, object|
				caller.location.announceExcluding(caller, "% says, \"%\"".format(caller.name,
					dobj.asString.stripEnclosingQuotes), caller);
				caller.postUser("You say,  \"%\"".format(dobj.asString.stripEnclosingQuotes), caller);
			}.asCompileString;
		);

		moo.generics[\player].verb_(\pose, \this, \any,
			{|dobj, iobj, caller, object|
				caller.location.announce("% %".format(caller.name, dobj.asString.stripEnclosingQuotes), caller);
			}.asCompileString;
		);

		moo.generics[\player].verb_(\inventory, \this, \none,

			{|dobj, iobj, caller, object|
				var str, last;
				//object.description.postln;
				(caller.contents.size == 0).if({
					str = "You are not holding anything.";
				}, {
					(caller.contents.size == 1).if({
						str = "You are holding: %.".format(caller.contents[0].name);
					}, {
						(caller.contents.size > 1).if({
							last = caller.contents.last;
							str =  "You are holding: % and %.".format(
								caller.contents.copyRange(0, caller.contents.size-2)
								.collect({|c| c.name }).asList.join(", "),
								last.name
							);
						})
					})
				});
				str.notNil.if({
					//str.debug(object);
					caller.postUser(str, caller);
				} , {
					"Should not be nil".warn;
				});
			}.asCompileString;

		);

		moo.generics[\player].verb_(\login, \this, \any,
			{|dobj, iobj, caller, object|
				var room;

				iobj.isKindOf(MooRoom).if({
					room = iobj;
				}, {
					room = object.moo.lobby;
				});

				caller.move(room, caller);
				caller.location.announceExcluding(caller, "With a puff of smoke and flash of light, % appears.".format(caller.name), caller);

			}.asCompileString;
		);

		moo.generics[\player].verb_(\give, \any, \this,

			{|dobj, iobj, caller, object|

				var str, success = false, oname = dobj, receiver = iobj;

				//"give".debug(object);

				object.isKindOf(MooPlayer).if({
					success = true;
					receiver = object.name;

					dobj.isKindOf(MooObject).if({
						oname = dobj.name;

						success = false; // this could go wrong

						((caller == object.owner) || caller.wizard).if({
							(dobj.owner != object).if({
								dobj.owner = object;
								success = true; // changed owners
							});
						});

						// or it's also a success if somebody else is now holding it
						(caller != object).if({
							success = (object.addObject(dobj, caller)) || success;
						});
					});
				});



				success.if({
					// tell onlookers
					str = "% gave % to %.".format(caller.name, oname, receiver);
					(caller.location != object.location).if({
						object.location.announceExcluding([caller, object], str, caller);
					});
					caller.location.announceExcluding([caller, object], str, caller);

					caller.postUser("You gave % to %.".format(oname, receiver), caller);
					object.postUser("% gave you %.".format(caller.name, oname), caller);
				} , {

					str = "% tried and failed to give % to %.".format(caller.name, oname, receiver);
					object.isKindOf(MooObject).if({
						// tell the recipient and their onlooks, if they exist
						(caller.location != object.location).if({
							object.location.announceExcluding([caller, object], str, caller);
						});
						object.postUser("% grandiosely tried to give you %, but failed.".format(caller.name, oname), caller);
					});

					// tell the caller's onlookers
					caller.location.announceExcluding([caller, object], str, caller);
					caller.postUser("You flailing tried to give % to %, but failed.".format(oname, receiver), caller);
				});



			}.asCompileString;

		);

	}




}

MooEnParser : MooParser {

	* initClass{

		//i18n[\make] = \make;
	}

	parse {|tokens|


		tokens.debug(this);

			//"flattened".postln;

			//tokens.postln;
			//tokens.debug(this);

			verb = tokens[0];
			(tokens.size > 1).if({ dobj = tokens[1]; });

			(tokens.size > 2).if ({ iobj = tokens.last; });

		//tokens.postln;
		//[verb, dobj, iobj].debug(this);

	}


}




MooEoParser : MooParser {


	parse {|tokens|

		var  unquoted, found;


		tokens.do({|token|

			found = false;

			token.endsWith("u").if({
				verb = token;
				found = true;
			});

			// the following only works if the verb is not "estu"

			(token.endsWith("on") ||
				token.endsWith("ojn")).if({
				dobj = token[0..(token.size-2)]; // chop off the n
				found = true;
			});

			(token.endsWith("o") ||
				token.endsWith("oj")).if({
				iobj = token;
				found = true;
			});

			// look out for #1234 style objects
			token.beginsWith("\#").if({

				token.endsWith("n").if({
					dobj = token[0..(token.size-2)]; // chop off the n
				}, {
					iobj = token;
				});
				found = true;
			});

			this.isString(token).if({
				unquoted = this.stripQuotes(token);
				unquoted.split($ ).do({|word|
					word.endsWith("n").if({
						dobj.isNil.if({
							dobj = token; // don't blow away the dobj we already found
						} , {
							iobj = token;
						});
						found = true;
					});
				});

				// verbs can't be quoted strings, so it must be an indirect object
				found.not.if({
					iobj = token;
				});
			});

		});

		// if we have found an iobj, but not a dobj, this is an error
		(dobj.isNil && iobj.notNil).if({
			dobj = iobj;
			iobj= nil;
		});
	}

}
