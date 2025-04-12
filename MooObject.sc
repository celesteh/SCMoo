MooObject : NetworkGui  {

	classvar >generic;

	var <moo,  owner, <id, <aliases, verbs, location,  <properties, <>immobel,
	superObj;

	*generic {|moo|
		var ret, moo_generics;

		// this method is expensive, especially with the recursion, however, it doesn't get called often

		ret = generic;
		ret.notNil.if({
			^ret
		});

		// search the moo
		moo.notNil.if({

			ret = moo.generics.values.detect({|item| item.class == this});
			ret.notNil.if({
				generic = ret;
				^ret;
			});

			moo_generics = moo.generics.values.collect({ |item| this.mooObject(item, moo) });
			ret = moo_generics.detect({|item| item.class == this});

			ret.notNil.if({
				generic = ret;
				^ret;
			});
		});

		// try a superclass
		this.asClass.superclass.findRespondingMethodFor(\generic).notNil.if({
			^super.generic(moo);
		})
		^nil;
	}

	*new { |moo, name, maker, parent|
		"MooObject.new".postln;
		^super.new.initMooObj(moo, name, maker, parent ? this.generic(moo));
	}

	*fromJSON{|dict, converter, moo|
		"fromJSON MooObject".debug(this);
		^super.new.restore(dict, converter, moo);
	}

	restore{|dict, converter, imoo |

		var json_id, name, parent, json_owner, owner_id, superID, props, public, key, value, verbs, verb;

		"restore".debug(this);

		moo = imoo ? Moo.default;

		moo.notNil.if({

			super.make_init(moo.api, nil, {});

			aliases = [];
			verbs = IdentityDictionary();
			properties = IdentityDictionary();
			immobel = false;


			name = dict.atIgnoreCase("name");
			id = dict.atIgnoreCase("id");
			id = moo.add(this, name, this.id);

			// sort out owner
			json_owner = dict.atIgnoreCase("owner");
			json_owner.notNil.if({
				(json_owner.asString != "null").if({
					owner_id = converter.getIDFromRef(json_owner, moo);
					owner_id.notNil.if({ json_owner = owner_id });
					(json_owner.asString.stripWhiteSpace == id.asString.stripWhiteSpace).if({
						owner = this;
					});
				}, {
					owner = this
				});
			}, {
				owner = this;
			});
			owner.isNil.if({ owner = json_owner ? owner_id });

			// sort out parent
			parent =  this.class.refToObject(dict.atIgnoreCase("parent")) ?
			{"parent not found".warn; moo.genericObject}.value;
			this.pr_superObj_(parent);


			props = dict.atIgnoreCase("properties");
			props.do({|prop|
				// each property is a dictionary
				public = prop.atIgnoreCase("public");
				prop.keys.do({|key|
					(key.asString.compare("public", true) != 0).if({
						value = prop.atIgnoreCase(key);
						this.property_(key.asSymbol, value, public, this);
					});
				});
			});

			verbs = dict.atIgnoreCase("verbs");
			verbs.do({|json_verb|
				verb = MooVer.fromJSON(json_verb, converter, moo, this);
				key = verb.verb;
				this.prValidID(key).not.if({
					MooError("Verbs must start with a letter and not cotnain special characters").throw;
				});
				verbs.put(key, verb);
			});

			location = this.class.refToObject(dict.atIgnoreCase("location"), converter, moo);
			aliases = aliases ++ dict.atIgnoreCase("aliases").collect({|a| a.asSymbol });
			immobel = dict.atIgnoreCase("immobel");

		});
	}

	restored {

		// call after moo restoration has finished

	}


	initMooObj {|imoo, iname, maker, parent|

		var /*name,*/ superID, superKey, public, str;

		// iff add is flase, this has been caled in a weird way

		"initMooObj imoo * ".format(imoo).debug(this);


		moo = imoo ? Moo.default;

		moo.notNil.if({

			// the first one is the generic
			this.class.generic.isKindOf(this.class).not.if({
				this.class.generic = this;
			});

			super.make_init(moo.api, nil, {});

			//moo = imoo;
			"maker is %".format(maker).debug(this);
			"testing %".format((maker == \this)).debug(this);

			((maker == \this)).if({
				owner = this;
				maker = this;
			} , {
				owner = maker;
			});

			"owner is %".format(owner).debug(this);

			aliases = [];
			verbs = IdentityDictionary();
			properties = IdentityDictionary();
			immobel = false;


			//super.make_init(moo.api, nil, {});

			"about to do some parent stuff".debug(this);
			this.pr_superObj_(parent);
			/*
			parent.notNil.if({
				parent.isKindOf(MooObject).if({
					superID = parent.id;
					superObj = parent;
				}, {
					superID = parent;
					//superID.isKindOf(SimpleNumber).if({
					(superID.isKindOf(String) || superID.isKindOf(Symbol)).if({
						superObj = moo.at(superID.asSymbol);
					});
				});

				this.property_(\parent, superID, false, maker);
			});
			*/

			//playableEnv = NetworkGui.make(moo.api);
			//playableEnv.know = true;

			"about to copy properties".debug(this);
			this.pr_copyParentProperties();


			//playableEnv = NetworkGui.make(moo.api);
			//playableEnv.know = true;

			str = superID.asString;
			id = (str.copyRange(str.size - 2.rrand(8),str.size-1) ++ Date.getDate.rawSeconds.asString)
			.select({|d|
				d.isDecDigit;
			}).asString.copyRange(0, 17).asSymbol;
			//(superID.asString ++ this.identityHash.asString).asSymbol;
			id = moo.add(this, iname, this.id);
			name = iname ? id.asString;

			name.debug(this);

			this.property_(\name, name, true, maker);//.action_(this, {|v| this.name = v.value });//.value.postln;

			this.property(\description).isNil.if({
				this.property_(\description, "You see nothing special.", true, maker);
			});

			this.verb(\look).isNil.if({
				this.verb_(\look, \this, \none,

					{|dobj, iobj, caller, object|
						object.description.postln;
						caller.postUser(object.description.value);
					}.asCompileString;

				);
			});

			this.verb(\describe).isNil.if({
				this.verb_(\describe, \this, \any,

					{|dobj, iobj, caller, object|

						(caller == object.owner).if({
							//object.description.value_(iobj.asString);
							object.property_(\description, iobj.asString.stripEnclosingQuotes, false, caller);
						});
					}.asCompileString;

				);
			});

			this.verb(\drop).isNil.if({
				this.verb_(\drop, \this, \none,

					{|dobj, iobj, caller, object|
						//caller.contents.remove(dobj);
						caller.remove(dobj);
						caller.location.announce("% dropped %".format(caller.name, dobj.name), caller);
						//caller.location.contents = caller.location.contents.add(dobj);
						caller.location.addObject(dobj);
					}.asCompileString;

				);
			});
		});
	}


	pr_superObj_{|parent|
		var superID;

		//	genericPlayer.isKindOf(MooObject).if({
		//		superID = genericPlayer.id;
		//		superObj = genericPlayer;
		//	}, {
		//		superID = genericPlayer;
		//		superObj = moo.at(superID);
		//	});
		//	this.property_(\parent, superID, false);
		//});


		parent.notNil.if({
			parent.isKindOf(MooObject).if({
				superID = parent.id;
				superObj = parent;
			}, {
				superID = parent;
				//superID.isKindOf(SimpleNumber).if({
				(superID.isKindOf(String) || superID.isKindOf(Symbol)).if({
					superObj = moo.at(superID.asSymbol);
					superObj.isNil.if({ superObj = superID; });
				});
			});

			this.property_(\parent, superID, false, owner);
		});
	}

	// obj could be a moo object or an ID string and we don't know which
	*mooObject{|obj, moo|
		var relevantID;

		obj.isNil.if({
			^nil
		});

		obj.isKindOf(MooObject).if({
			^obj
		});
		relevantID = obj;
		obj = moo.at(relevantID.asSymbol);
		obj.notNil.if({
			^obj
		});
		obj = relevantID;
		^obj;
	}

	*refToObject{|obj, converter, moo|
		var id, json_obj;
		//owner =  dict.atIgnoreCase("owner");
		obj.isNil.if({
			^nil
		});

		id = converter.getIDFromRef(obj);
		id.notNil.if({
			obj = moo.at(id);
		}, {
			//json_obj = moo.at(obj);
			//json_obj.notNil.if({
			//	obj = json_obj;
			//});
			obj = this.mooObject(obj, moo);
		});
		^obj;
	}

	// obj could be a moo object or an ID string and we don't know which
	*id{|obj|
		obj.isKindOf(MooObject).if({
			^obj.id;
		});
		(obj.isKindOf(String) || obj.isKindOf(Symbol)).not.if({
			obj = obj.asString;
		});
		^obj.asSymbol;
	}

	// this very dodgy method sorts out circular dependancies
	pr_resolve{|obj|
		^this.class.mooObject(obj, moo);
	}

	pr_superObj {
		^this.pr_resolve(superObj);
	}

	owner{
		^this.pr_resolve(owner)
	}

	location{
		^this.pr_resolve(location)
	}


	formatKey{|key|

		^"%/%".format(key, id).asSymbol;
	}

	pr_copyParentProperties{

		var public, superObject;

		superObj.notNil.if({

			superObject = this.pr_superObj;

			//superObj.notNil.if({
			superObject.isKindOf(MooObject).if({
				// copy the parent's properties
				this.immobel = superObject.immobel;
				superObject.properties.keys.do({|key|
					this.properties[key].isNil.if({
						// is it public?
						public = superObject.isPublic(key);
						this.property_(key, superObject.property(key).value, public);
					});
				});
			});
		});

	}

	prValidID {|key|
		var naughty_count = 0, valid = true;

		key.isKindOf(Symbol).if({ key = key.asString });

		key.isKindOf(String).if({
			valid = MooVerb.pass(key);

			// weird chars
			naughty_count = key.sum({|char| (char.isAlphaNum).if({ 0 } , { 1 }) });
			(naughty_count > 0).if({ valid = false });

			// starts with a number
			key[0].isAlpha.not.if({ valid = false; });

				// reserved word
				(key.compare("public", true) == 0).if({
					valid = false;
				});
		} , {
			// whatever else is happening here is weird and I don't like it
			valid = false;
		});

		^valid;
	}

	alias {|new_alias|

		//new_alias.isKindOf(SimpleNumber).if({
		this.prValidID(new_alias).not.if({
			MooError("Alias must start with a letter and not contain special characters").throw;
		});

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

		//key.isKindOf(SimpleNumber).if({
		//	MooError("Property can't be a number").throw;
		this.prValidID(key).not.if({
			MooError("Property must start with a letter and not contain special characters").throw;
		});

		key = key.asSymbol;

		//"property_ % %".format(key, ival).postln;

		verbs.includesKey(key).if({
			MooError("% name already in use by %".format(key, this.name)).throw;
		});

		properties.includesKey(key).if({
			// overwrite. Send notification
			//properties.at(key).value_(ival, changer);
			shared = properties.at(key);
			shared.value_(ival, changer);
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

		//properties.keys.postln;
		//"saved as %".format(properties[key].value).postln;

		^shared;
	}

	property {|key|
		var value, superObj;

		key = key.asSymbol;

		value = properties.at(key);

		value.isNil.if({
			superObj = this.pr_superObj;
			superObj.isKindOf(MooObject).if({
				value = superObj.property(key);
			});
		});

		^value
	}


	verb_ {|key, dobj, iobj, func, publish=false|

		var newV;

		//key.isKindOf(SimpleNumber).if({
		//	MooError("Verb can't be a number").throw;
		this.prValidID(key).not.if({
			MooError("Verbs must start with a letter and not cotnain special characters").throw;
		});


		"New verb %".format(key).postln;

		key = key.asSymbol;

		// We can blow away existing verbs, but not if the name is use by a property
		((properties.includesKey(key))/* || verbs.includesKey(key)*/).if({
			MooError("% name already in use by %".format(key, this.name)).throw;
		}, {
			newV = MooVerb(key, dobj, iobj, func, this, publish);

			verbs.put(key, newV);
		});
	}

	localVerb{|key|
		^verbs.at(key.asSymbol);
	}


	getVerb {|key|

		var superObj,verb = this.localVerb(key);


		verb.isNil.if({
			superObj = this.pr_superObj;
			superObj.isKindOf(MooObject).if({
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

		this.dumpBackTrace;

		selector = selector.asSymbol;

		if(this.respondsTo(selector), {
			^this.perform(selector, *args);
		});

		// first try moo stuff
		//"..\ndoesNotUnderstand %".format(selector).debug(this.id);
		//verbs.keys.postln;
		//properties.keys.postln;
		//"..".postln;

		verb = this.getVerb(selector);//verbs[selector];
		if (verb.notNil) {
			"verb %".format(selector).postln;
			//^func.functionPerformList(\value, this, args);
			^verb.invoke(args[0], args[1], args[2], args[3]);
		};

		"% is not a verb".format(selector).debug(this);
		verbs.keys.postln;

		if (selector.isSetter) {
			selector = selector.asGetter;
			if(this.respondsTo(selector), {
				warn(selector.asCompileString
					+ "exists as a method name, so you can't use it as a pseudo-method.");
				this.dumpBackTrace;
				{
					this.perform(selector.asSetter, *args);
					//"new result is %".format(this.perform(selector)).debug(this.id);
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

	getClock {|quant|
		var clock;
		clock = this.property(\clock);
		clock.isKindOf(MooClock).if({
			quant = quant ? clock.quant;
			^[clock.clock, quant];
		});

		location.notNil.if({
			^location.getClock(quant);
		});

		^nil
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

		^false;
	}

	toJSON{|converter|

		var encoder, props;

		//"MooObject.toJSON".debug(this);

		converter.isNil.if({
			encoder = MooCustomEncoder();
			//^MooJSONConverter.convertToJSON(this, encoder);
			converter = MooJSONConverter(customEncoder:encoder);
		});

		^"{ % }".format(this.pr_JSONContents(converter));


	}

	pr_JSONContents {|converter|

		var public, str="", synths ="", props, json_verbs; //properties.collect({|p| converter.convertToJSON(p) });



		props = properties.keys.collect({|key|
			public = this.isPublic(key);
			"{ \"%\" : %, \"public\" : % }".format(key, converter.convertToJSON(properties[key]), public);
		}).asList;

		//"MooObject.prJSONCONTENTS".debug(this);

		json_verbs = verbs.keys.collect({|key|
			converter.convertToJSON(verbs.at(key))
		}).asList;



		synthDefs.notNil.if({
			//synths = ", \"synths\" : %,".format(converter.convertToJSON(synthDefs));
		}, {
			synths = "";
		});

		//"id %".format(this.id.asString).debug(this);

		str = "\"id\" : \"%\", ".format(this.id.asString);
		str = str + "\"class\" : \"%\",".format(this.class);
		str = str + "\"name\" : \"%\", ".format(this.name);
		str = str + "\"verbs\" : [ % ],".format(json_verbs.join(", "));
		str = str + "\"properties\" : [ % ],".format(props.join(", ")); //format(converter.convertToJSON(props)) ;
		str = str + "\"aliases\" : %,".format(converter.convertToJSON(aliases)) ;
		str = str + "\"location\" : %,".format(location !? { converter.convertToJSON(location.id) } ? "null") ;
		str = str + "\"immobel\" : %,".format(immobel.asCompileString) ;
		str = str + synths;
		str = str + "\"owner\": % ".format(owner !? {converter.convertToJSON(owner.id)} ? "null") ;

		//str.debug(this);

		^str


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

			{|dobj, iobj, caller|

				sharedTempo.value = iobj.asFloat;
			}.asCompileString;

		); //uses a property so doesn't need to be published

		this.verb_(\play, \this, \any,

			{|dobj, iobj, caller|

				clock.play
			}.asCompileString;

			, true);

		this.verb_(\stop, \this, \any,

			{|dobj, iob, caller|

				clock.stop
			}.asCompileString;

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


			{|dobj, iobj, caller|

				(caller == owner).if({
					dobj.isKindOf(MooPlayer).if({
						players.includes(dobj).not ({
							players = players.add(dobj);
						})
					})
				})
			}.asCompileString;

		);

	}
}

MooContainer : MooObject {

	var <contents, semaphore;

	*new { |moo, name, maker, parent|

		^super.new(moo, name, maker, parent ? this.generic(moo)).initContainer();
	}

	*fromJSON{|dict, converter, moo|
		"fromJSON MooContainer".debug(this);
		^super.fromJSON(dict, converter, moo).containerRestore(dict, converter, moo);
	}


	initContainer{

		semaphore = Semaphore(1);
		contents = [];
		//immobel = superObj.immobel;

		this.verb(\inventory).isNil.if({
			this.verb_(\inventory, \this, \none,

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
						str.debug(object);
						caller.postUser(str);
					} , {
						"Should not be nil".warn;
					});
				}.asCompileString;

			);
		});

	}

	remove {|item|

		var key;

		"remove %".format(item).debug(this.class);

		semaphore.wait;

		"waited".debug(this);

		contents.remove(item);

		"removed from contents".debug(this.class);

		//playableEnv.remove(item);
		key = this.findKeyForValue(item);
		"key %".format(key).debug(this.class);
		//super.remove(item);
		key.notNil.if({
			this.removeAt(key);

			"removed from environment".debug(this.class);
		});

		semaphore.signal;

		"signaled".debug(this.class);

	}

	addObject {|item|

		semaphore.wait;
		contents = contents.add(item);
		//playableEnv.put(item.name.asSymbol, item);
		this.put(item.name.asSymbol, item);
		semaphore.signal

	}

	findObj {|key|

		key = key.asSymbol;
		//var found;

		//contents.do({|obj|

		//	obj.matches(key).if({

		//		found = obj;
		//	})
		//});

		//^found;
		^contents.detect({|obj| obj.matches(key) });
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

	pr_JSONContents {|converter|

		var stuff;


		stuff = contents.collect({|c| c !? { c.id } ? "null" });

		^super.pr_JSONContents(converter) +
		",\"contents\":  % " .format(converter.convertToJSON(stuff));
	}

	containerRestore{ |dict, converter, moo|

		var json_contents;

		semaphore = Semaphore(1);
		contents = [];


		json_contents = dict.atIgnoreCase("properties");
		contents = contents ++ json_contents.collect({|item| this.refToObject(item, converter, moo) });
	}


	restored {

		semaphore.wait;
		contents.do({|item|
			this.put(item.name.asSymbol, item);
		});
		semaphore.signal

	}



}

MooRoom : MooContainer {

	var<players, <exits;

	*new { |moo, name, maker, parent|

		^super.new(moo, name, maker, parent ? this.generic(moo)).initRoom();
	}

	*fromJSON{|dict, converter, moo|
		"fromJSON MooRoom".debug(this);
		^super.fromJSON(dict, converter, moo).roomRestore(dict, converter, moo);
	}

	roomRestore{ |dict, converter, moo|

		var json_exits, key, value;

		//semaphore = Semaphore(1);
		players = [];
		//contents = [];
		exits = IdentityDictionary();

		//departures = exits.keysValuesDo({|key, val| "{ \"key\": \"%\", \"val\": %}".format(key, val !? {val.id} ? "null") }).asList;

		json_exits = dict.atInoreCase("exits");
		json_exits.do({|item|
			key = item.atIgnoreCase("key");
			value = item.atIgnoreCase("val");
			value = MooObject.refToObject(value, converter, moo);
			exits.put(key.asSymbol, value);
		});
	}


	initRoom {

		//super.initMooObj(true, moo, name, maker);
		players = [];
		aliases = ["here"];
		exits = IdentityDictionary();
		immobel = true;

		this.verb(\announce).isNil.if({

			this.verb_(\announce, \any, \this,
				// announce "blah" to here

				{|dobj, iobj, caller|

					iobj.announce(dobj, caller);
				}.asCompileString;

			);
		});

		this.verb(\arrive).isNil.if({
			this.verb_(\arrive, \any, \this,

				{|dobj, iobj, caller|
					"arrive".postln;
					caller.isPlayer.if({

						iobj.announce("With a dramatic flourish, % enters".format(caller.name));
						//players = players.add(caller);
						caller.dumpStack;
						iobj.addPlayer(caller);
						caller.location = iobj;
						iobj.getVerb(\look).invoke(iobj, iobj, caller, iobj);
					});

				}.asCompileString;

			);
		});

		this.verb(\depart).isNil.if({
			this.verb_(\depart, \any, \this,

				{|dobj, iobj, caller|
					caller.isPlayer.if({

						//players.remove(caller);
						iobj.removePlayer(caller);
						iobj.announce("With a dramatic flounce, % departs".format(caller.name));

					});
				}.asCompileString;

			);
		});
	}

	announce {|str, caller|

		var tell;

		players.do({|player|
			player.postUser(str, caller);
		});

		contents.do ({|thing|
			//thing.verbs.includesKey(\tell).if({
			tell = thing.getVerb(\tell);
			tell.notNil.if({
				//tell = thing.verbs.at(\tell);
				tell.invoke(thing, str, caller, thing);
			});
		});
	}

	announceExcluding{|excluded, str, caller|
		var tell;

		players.do({|player|
			(player != excluded).if({
				player.postUser(str);
			});
		});

		contents.do ({|thing|
			//thing.verbs.includesKey(\tell).if({
			tell = thing.getVerb(\tell);
			tell.notNil.if({
				//tell = thing.verbs.at(\tell);
				tell.invoke(thing, str, caller, thing);
			});
		});
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
		^MooObject.mooObject(exits[\key] , moo);
	}

	addExit{|key, room|

		exits.put(key, room);
	}

	findObj {|key|

		var found;//, search;
		key = key.asSymbol;
		/*
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
		*/

		found = super.findObj(key);
		found.isNil.if({
			found = players.detect({|obj| obj.matches(key) });
		});
		^found;

	}

	env {

		//^playableEnv
	}


	remoteDesc { arg user;
		// ??

	}

	pr_JSONContents {|converter|

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

		//"exits %".format(exits.keys).debug(this);

		//stuff = contents.collect({|c| c !? { c.id } ? "null" });
		departures = exits.keysValuesDo({|key, val| "{ \"key\": \"%\", \"val\": %}".format(key, val !? {val.id} ? "null") }).asList.join(", ");

		^super.pr_JSONContents(converter) +
		//",\"contents\":  % ," .format(converter.convertToJSON(stuff)/*stuff.join(", ")*/) +
		", \"exits\": [ % ]".format(departures);//.format(converter.convertToJSON(departures)/*departures.join(",\n")*/);
	}




}
