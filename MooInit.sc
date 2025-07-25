MooInit {

	// This class is for starting a new moo database from scratch


	*createMap {|dict, moo|

		var rooms, place, destination;

		// This dictionary looks like
		//\london, [\north, \newcastle, \east, \essex, \west, \wales, \south, \surrey]

		rooms = IdentityDictionary();

		// first make all the rooms
		dict.keys.do({|key|

			key = key.asSymbol;
			((key != \lobby) && (key != \Lobby)).if({
				rooms.put(key, MooRoom(moo, key.asString, moo.me, moo.generics[\room]));
			} , {
				rooms.put(key, moo.lobby);
			});
		});

		// then make all the exits and any leaf nodes not mentioned earlier
		dict = dict.collect({|item, key|
			place = rooms.at(key.asSymbol);

			item.pairsDo({|exit, where|

				[exit, where].debug(key);

				exit = exit.asSymbol;
				where = where.asSymbol;

				//location.addExit(dobj, thing);
				destination = rooms.at(where);
				destination.isNil.if({
					destination = MooRoom(moo, where.asString, moo.me, moo.generics[\room]);
					rooms.put(where, destination);
				});
				place.addExit(exit, destination);
			});
		});

		^rooms
	}


	*initAll {|moo, i18n|


		var root;

		i18n  = i18n ? Moo.localisation;

		// Make root
		//ok, the roo ID is always \0

		root = MooRoot(moo, "Root");
		//"made root".debug(this);

		moo.generics[\object] = MooObject(moo, i18n.at(\object).asString, root, -1); //object
		//moo.generics[\object] = MooObject(moo, "object", root, -1);
		moo.generics[\object].description_(i18n.at(\defaultDesc).asString); //"You see nothing special."
		this.updateGenericObject(moo);

		moo.generics[\container] = MooContainer(moo, i18n.at(\bag).asString, root, moo.generics[\object]); //bag
		this.updateGenericContainer(moo);

		//"make a generic player".debug(this);
		moo.generics[\player] = MooPlayer(moo, i18n.at(\player).asString, nil, false, moo.generics[\container]); // player
		//"made generic player, %".format(generics[\player].name).debug(this);
		this.updateGenericPlayer(moo);

		// now set this as the parent of root
		root.parent = moo.generics[\player];


		moo.generics[\room] = MooRoom(moo, i18n.at(\room).asString, root, moo.generics[\container]); // room
		moo.generics[\room].description_(i18n.at(\defaultRoomDesc).asString); //"An unremarkable place."

		//MooParser.reserveWord(\room, genericRoom);
		//generics[MooRoom] = generics[\room];

		moo.lobby = MooRoom(moo, i18n.at(\Lobby).asString, root, moo.generics[\room]); //Lobby
		this.updateGenericRoom(moo);
		//me = root;



		//objects = [MooRoom(this, "Lobby", objects[0])];
		//index = 2;

		MooParser.reserveWord(\l, \look);
		MooParser.reserveWord(\inv, \inventory);

		// look with no args
		MooParser.reserveWord(\look, [\look, \here]);
		// inv with no args
		MooParser.reserveWord(\inventory, [\inventory, \me]);

	}


	*updateAll{|moo, i18n|

		i18n.isNil.if({
			this.updateGenericObject(moo);
			this.updateGenericContainer(moo);
			this.updateGenericPlayer(moo); // also gets container and object
			this.updateGenericRoom(moo); // twice over, but nbd
		} , {
			i18n.respondsTo(\updateGenericObject).if({
				i18n.updateGenericObject(moo);
			}, {
				this.updateGenericObject(moo);
			});

			i18n.respondsTo(\updateGenericContainer).if({
				i18n.updateGenericContainer(moo);
			}, {
				this.updateGenericContainer(moo);
			});

			i18n.respondsTo(\updateGenericPlayer).if({
				i18n.updateGenericPlayer(moo);
			}, {
				this.updateGenericPlayer(moo);
			});

			i18n.respondsTo(\updateGenericRoom).if({
				i18n.updateGenericRoom(moo);
			}, {
				this.updateGenericRoom(moo);
			});
		});

	}


	*updateGenericObject{|moo|

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

	*updateGenericContainer{|moo|

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

	*updateGenericRoom{|moo|

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

	*updateGenericPlayer{|moo|



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