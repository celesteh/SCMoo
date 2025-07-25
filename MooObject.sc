MooProperty {

	var cv, <>mutable;

	*new {|cv, mutable|
		^super.newCopyArgs(cv, mutable);
	}

	value_ {|...args|
		mutable.if({
			cv.value_(*args);
		});
	}

	value {
		^cv.value;
	}

	silentValue_{|...args|
		mutable.if({
			cv.silentValue_(*args);
		});
	}

	silent_{|bool|
		cv.silent_(bool);
	}

	slient { ^cv.bool }

	action_ { |arg1, arg2|
		arg1 = arg1 ? \local;
		cv.action_(arg1, arg2);  //this.action_(action_owner, {})
	}

	toJSON{|converter|

		^converter.convertToJSON(cv)
	}

	doesNotUnderstand { arg selector ... args;
		^cv.value.perform(selector, *args);
	}
}

MooObject : NetworkGui  {

	//classvar >generic;

	var <moo,  owner, <id, <aliases, <verbs, /*location,*/  <properties, <>immobel,
	superObj;

	// NOTE: there's a reason there's no getter for location. There's a method below!

	*generic {|moo|
		var ret, moo_generics;

		// this method is expensive, especially with the recursion, however, it doesn't get called often

		//ret = generic;
		//ret.notNil.if({
		//	^ret
		//});

		// search the moo
		moo = moo ? Moo.default;

		moo.notNil.if({

			ret = moo.generics.values.detect({|item| item.class == this});
			ret.notNil.if({
				//generic = ret;
				^ret;
			});

			moo_generics = moo.generics.values.collect({ |item| this.mooObject(item, moo) });
			ret = moo_generics.detect({|item| item.class == this});

			ret.notNil.if({
				//generic = ret;
				^ret;
			});
		});

		// try a superclass
		this.asClass.superclass.findRespondingMethodFor(\generic).notNil.if({
			^super.generic(moo);
		})
		^nil;
	}

	*new { |moo, name, maker, parent, local=false, id, location|
		"new obj id % local %".format(id, local).debug(this);
		^super.new.initMooObj(moo, name, maker, parent ? this.generic(moo), local, id, location);
	}

	*fromJSON{|dict, converter, moo|
		//"fromJSON MooObject".debug(this);
		^super.new.restore(dict, converter, moo);
	}

	restore{|dict, converter, imoo |

		var json_id, parent, json_owner, owner_id, superID, props, public, key, value, jverbs, verb,
		getObj, mutable, location, process;

		//"restore %".format(dict).debug(this);

		moo = imoo ? Moo.default;

		getObj = {|key|

			var oid, obj, j_obj;

			j_obj = dict.atIgnoreCase(key);

			//"restore: Is in dict? %".format(j_obj).debug(this.id);

			j_obj.notNil.if({
				(j_obj.asString != "null").if({
					//"not null %".format(j_obj).debug(this);
					//oid = converter.getIDFromRef(j_obj, moo);
					oid = this.class.refToObject(j_obj, converter, moo);
					//"id %".format(oid).debug(this);
					oid.notNil.if({ j_obj = oid });
					(j_obj.asString.stripWhiteSpace == id.asString.stripWhiteSpace).if({
						obj = this;
					}, {
						j_obj.respondsTo(\id).if({
							(j_obj.id.asString.stripWhiteSpace == id.asString.stripWhiteSpace).if({
								obj = this;
							});
						});
					});
				}, {
					//obj = this
				});
			}, {
				//obj = this;
			});
			//obj.isNil.if({ obj = j_obj ? oid ? key });

			obj = obj ? j_obj? oid ? key;

			obj;
		};


		process = {|item|

			var obj, result;

			// is it a Moo object
			obj = getObj.(item);//MooObject.mooObject(value, moo);
			obj.isKindOf(MooObject).if({
				result = obj.id;
			});

			result.isNil.if({

				// is it a number?
				item.isKindOf(String).if({
					item = item.stripEnclosingQuotes;

					item.isDecimal.if({
						item.contains($.).if({
							result = item.asFloat;
						} , {
							result = item.asInteger;
						});
					});
				}, {
					// already know this is not a string
					item.isKindOf(ArrayedCollection).if({

						result = item.collect({|i|
							process.(i);
						});
					} , {
						// this might be dodgy, idk
						item.isKindOf(Dictionary).if({
							result = item.keysValuesChange({|key, val| process.(val); });
						});
					});
				});
			});

			result ? item;
		};


		moo.notNil.if({

			super.make_init(moo.api, nil, {});
			this.sharePlayers = true;


			// <moo,  owner, <id, <aliases, verbs, location,  <properties, <>immobel, superObj;


			aliases = [];
			verbs = IdentityDictionary();
			properties = IdentityDictionary();
			immobel = false;

			manyPlayers = false;


			name = dict.atIgnoreCase("name");
			//"name %".format(name).debug(this);
			id = dict.atIgnoreCase("id");
			//"id %".format(id).debug(this);
			id = moo.add(this, name, id);


			owner = getObj.value("owner");
			owner = owner ? this; // if it's nil, we own ourselves

			//"owner % %".format(owner).debug(this);

			//"-------------------------------------------------------------".debug("Restore Properties");

			//  "properties" : [ { "parent" : 8521, "public" : false },  etc ]
			props = dict.atIgnoreCase("properties");
			props.do({|prop|



				// each property is a dictionary, with two keys

				// first find out if it's public
				public = prop.atIgnoreCase("public");
				public = public.asBoolean;
				mutable = prop.atIgnoreCase("mutable");
				mutable.notNil.if({
					mutable = mutable.asBoolean;
				});

				// now go through the keys
				prop.keys.do({|key|
					// publish the keys (not counting public)
					((key.asString.compare("public", true) != 0) &&
						(key.asString.compare("mutable", true) != 0))
					.if({

						value = prop.atIgnoreCase(key);

						/*
						//"restore: key % value %".format(key, value).debug(this.id);

						// does the vlaue refer to a MooObject?
						value= getObj.(value);//MooObject.mooObject(value, moo);
						value.isKindOf(MooObject).if({
							value = value.id;
						});

						// check for numbers hiding in strings
						value.isKindOf(String).if({
							value = value.stripEnclosingQuotes;
							value.isDecimal.if({
								value.contains($.).if({
									value = value.asFloat;
								} , {
									value = value.asInteger;
								});
							});
						});
						*/
						value = process.(value);

						// the changer is the JSON thingee
						this.property_(key.asSymbol, value, public, converter, mutable, silent:true);

						//"key % value % public % (is a %)".format(key, value, public, public.class).debug(this.class);
					});
				});
			});

			name = this.property(\name).value;

			this.property(\parent).mutable = false;
			parent = this.class.refToObject(this.property(\parent).value, converter, moo);
			parent = parent ? this.class.generic(moo) ? moo.genericObject;
			this.pr_superObj_(parent);



			jverbs = dict.atIgnoreCase("verbs");
			jverbs.do({|json_verb|
				verb = MooVerb.fromJSON(json_verb, converter, moo, this);
				key = verb.verb;
				//this.prValidID(key).not.if({
				MooVerb.validID(key).not.if({
					MooError("Verbs must start with a letter and not cotnain special characters").throw;
				});
				verbs.put(key, verb);
			});

			dict.atIgnoreCase("location").notNil.if({

				location = this.class.refToObject(dict.atIgnoreCase("location"), converter, moo);
				this.handleLocationInit(location);
			});


			aliases = aliases ++ dict.atIgnoreCase("aliases").collect({|a| a.asSymbol });
			immobel = dict.atIgnoreCase("immobel").asBoolean;

			//"MooObject.restore done".debug(this);

		});
	}

	restored {

		var superobj, location;

		// call after moo restoration has finished
		superobj = MooObject.mooObject(superObj, moo);
		this.pr_superObj_(superobj);
		location =  MooObject.mooObject(this.property(\location).value, moo);
		this.location = location;

		(location.notNil && (location.isKindOf(MooObject))).if({
			this.location.addObject(this, moo);
		});

		owner =  MooObject.mooObject(owner, moo);
		this.unsilence();
		this.networking();

	}

	handleLocationInit{|initialLoc, changer, local=false|

		var quiet;

		quiet = api.silence;
		api.silence = local.not;

		changer = changer ? moo;

		initialLoc.isNil.if({
			initialLoc = -1;
		});

		initialLoc.isKindOf(MooObject).if({
			initialLoc = initialLoc.id;
		});

		this.property_(\location, initialLoc, changer:changer, silent:true);
		//this.action_(action_owner, {})
		this.property(\location).action_(moo, {

			"!!! object moving!!!".debug("moo action");

			this.location.notNil.if({
				//this.location.isKindOf(MooObject).if({
				//this.location.addObject(this, moo);
				this.move(this.location, moo);
				//});
			});
		});

		api.silence = quiet;
	}


	initMooObj {|imoo, iname, maker, parent, local=false, in_id, location|

		var /*name,*/ superID, superKey, public, str, time, quiet, changer;

		local = local ? false;

		"local is %".format(local).debug(this);

		iname.notNil.if({

			iname = iname.asString.stripEnclosingQuotes;
			this.prValidID(iname).not.if({
				MooMalformedKeyError("% is not a valid ID".format(iname)).throw;
			});
		});

		aliases = [];
		verbs = IdentityDictionary();
		properties = IdentityDictionary();
		immobel = false;



		// iff add is flase, this has been caled in a weird way

		//"initMooObj imoo * ".format(imoo).debug(this);


		moo = imoo ? Moo.default;

		moo.notNil.if({

			// the first one is the generic
			//this.class.generic.isKindOf(this.class).not.if({
			//	this.class.generic = this;
			//});

			super.make_init(moo.api, nil, {});
			manyPlayers = false;
			this.sharePlayers = true;


			//moo = imoo;
			//"maker is %".format(maker).debug(this);
			//"testing %".format((maker == \this)).debug(this);

			((maker == \this)).if({
				owner = this;
				maker = this;
				//"we made ourselves!".debug(this.name);
			} , {
				owner = maker;
			});

			//"owner is %".format(owner).debug(this);

			// ok, get the ID very early on
			"in_id %".format(id).debug(this);

			id = in_id;
			id.isNil.if({
				str = superID.asString.copyRange(str.size - 4,str.size-1);
				time = ((Date.getDate.rawSeconds * 10) + 1000.rand).ceil.asString.copyRange(7,10);
				//"time %".format(time).debug(this);
				//id = (str.copyRange(str.size - 2.rrand(8),str.size-1) ++ Date.getDate.rawSeconds.asString)
				id = (str ++ time).select({|d|
					d.isDecDigit;
				}).asString;//.copyRange(0, 17).asInteger;
				//"id is % ".format(id).debug(this);
				id = id.copyRange(id.size - 8, id.size);
				//"id is % ".format(id).debug(this);
				//(superID.asString ++ this.identityHash.asString).asSymbol;
			});

			id = moo.add(this, iname, this.id);
			name = iname ? id.asString;

			//name.debug(this);

			// got ID and name;





			//"about to do some parent stuff %".format(parent).debug(this);
			//{ "%".format(parent.name).debug(this); }.try;
			this.pr_superObj_(parent);
			//"about to copy properties".debug(this);
			this.pr_copyParentProperties(parent, local);


			//playableEnv = NetworkGui.make(moo.api);
			//playableEnv.know = true;


			this.property_(\name, name, true, maker, silent:true);//.action_(this, {|v| this.name = v.value });//.value.postln;


			local.if({ changer = maker}, {changer = moo.api});
			this.handleLocationInit(location, changer, local);



			/*
			location = location ? -1;
			this.property(\location).isNil.if({
			quiet = api.silence;
			api.silence = local.not;
			this.property_(\location, location, changer:changer); // don't announce a wrong location
			//this.action_(action_owner, {})
			api.silence = quiet;
			});
			this.property(\location).action_(moo.api, {
			"put in %".format(this.location).debug(name);
			this.location.notNil.if({
			(this.location.isKindOf(MooObject)).if({
			this.location.addObject(this, moo.api);
			});
			});
			});
			*/


			local.if({

				//"LOCAL".debug("new object");

				parent = this[\parent].value;

				this.pr_copyParentProperties(parent, local);

				"parent % % %".format(this, this.name, parent).debug(this);

				parent.isKindOf(MooObject).if({
					parent = parent.id;
				});
				owner = this.owner;
				owner.isKindOf(MooObject).if({
					owner = owner.id;
				});

				api.sendMsg('newObject', this.id, name, this.class, parent, owner, api.nick, location);
			});

			this.property(\description).isNil.if({
				this.property_(\description, "You see nothing special.", true, maker, silent:true);
			});


			this.networking();
			this.unsilence();

		}); // end test for nil
	}


	networking {|silent = false|


		//key, ival, publish = true, changer, mutable=true, guitype|
		api.add("property/%".format(this.id).asSymbol,
			{arg key, ival, publish = true, changer, mutable=true, guitype, origin;
				(origin != moo.api.nick).if({
					this.property_(key, ival, publish = true, changer, mutable=true, guitype);
				});
		});


		//func.value(time, this, input);
		//key, ival, publish = true, changer, mutable=true, guitype|
		//api.add("property/%".format(this.id).asSymbol,
		//	{|time, resp, input|
		//		input.debug(this.id);
		//		{arg key, ival, publish = true, changer, mutable=true, guitype, origin;
		//			(origin != moo.api.nick).if({
		//				this.property_(key, ival, publish = true, changer, mutable=true, guitype);
		//			});
		//		}.value(*input);
		//});

		//moo.api.sendMsg ("move/%".format(this.id).asSymbol, newLocation.id, oldId, moo.api.nick);
		api.add("move/%".format(this.id).asSymbol, {|where, whence, origin|
			var from;

			"got move request % % %".format(whence, where, origin).debug(this.name);

			//(origin.toString.compare(moo.api.nick.toString) != 0).if({
			(origin != moo.api.nick).if({
				"not sent by me".debug(this.name);
				{
					whence.notNil.if({
						whence= whence.asInteger;
						(whence > 0).if({
							from = whence;
						})
					})
				}.try;

				this.move(where, moo.api, from);
			});
		});

	} // end networking()



	advertise {|property, key, publish, guitype, silent = true|
		silent.not.if({
			"advertising".debug(this.id);
			property.notNil.if({
				api.sendMesg("property/%".format(this.id).asSymbol, key, property.value, publish, api.nick,
					property.mutable, guitype);
			});
		});

	}

	unsilence {

		super.unsilence();

		this.properties.keys.do({|key|
			properties.at(key).silent = false;
		});
	}


	pr_superObj_{|parent|
		var superID;


		parent.notNil.if({
			parent.isKindOf(MooObject).if({
				superID = parent.id;
				superObj = parent;
				//"parent name %".format(superObj.name).debug(this);
			}, {
				superID = parent;
				//superID.isKindOf(SimpleNumber).if({
				//(superID.isKindOf(String) || superID.isKindOf(Symbol)).if({
				//	superObj = moo.at(superID.asSymbol);
				//	superObj.isNil.if({ superObj = superID; });
				//});
				superObj = MooObject.mooObject(superID, moo);
				superObj.isNil.if({ superObj = superID; });
			});

			this.property_(\parent, superID, false, owner, false, \none);
		});

		//"parent is %".format(superID).debug(this);
	}

	// obj could be a moo object or an ID string and we don't know which
	*mooObject{|obj, moo|
		var relevantID;

		obj.isNil.if({
			^nil
		});

		obj.isKindOf(MooProperty).if({
			obj = obj.value;
		});

		obj.isKindOf(MooObject).if({
			^obj
		});
		relevantID = obj;
		//obj = moo.at(relevantID.asSymbol);
		obj = moo.at(relevantID.asInteger);
		obj.notNil.if({
			^obj
		});
		obj = relevantID;
		^obj;
	}

	*refToObject{|obj, converter, moo|
		var id, found_obj;
		//owner =  dict.atIgnoreCase("owner");

		//"refToObject %".format(obj).debug(this);

		obj.isNil.if({
			^nil
		});

		converter.notNil.if({
			id = converter.getIDFromRef(obj);
			//"got an id %".format(id).debug(this);
		});

		id = id ? obj; // if we have an ID, use it

		//"id is %".format(id).debug(this);

		found_obj = this.mooObject(id, moo);

		// if we found something, return it
		^(found_obj ? id);
	}

	// obj could be a moo object or an ID string and we don't know which
	*id{|obj|
		obj.isKindOf(MooObject).if({
			^obj.id;
		});
		//(obj.isKindOf(String) || obj.isKindOf(Symbol)).not.if({
		//	obj = obj.asString;
		//});
		^obj.asInteger;//.asSymbol;
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
		var loc = this.property(\location).value;
		((loc == -1) || ("-1".compare(loc.asString) == 0)).if({ ^ nil });
		^this.pr_resolve(loc);
	}


	formatKey{|key|

		//^"%/%".format(id, key).asSymbol;
		^Moo.formatKey(id, key);
	}

	pr_copyParentProperties{|parent, local=false|

		var public, superObject, mutable, silent;

		silent = local.not; // if local is true, then silent is false;

		superObj.isNil.if({
			//superObj =
			this.pr_superObj = parent;
		});

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
						mutable = superObject.property(key).mutable;
						this.property_(key, superObject.property(key).value, public, mutable:mutable, silent:silent);
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
			naughty_count = key.sum({|char|
				((char.isAlphaNum) || (char == $\ ) || (char == $_ )).if({ 0 } , { 1 })
			});
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
			//"weird".debug(this);
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


	location_{|loc, changer|

		changer = changer? moo.me;
		//location = loc;

		loc.isKindOf(MooObject).if({
			loc= loc.id;
		});

		loc.isNil.if({
			loc = -1;
		});

		"location %".format(loc).debug(this.id);
		this.property_(\location, loc, changer:changer);
	}

	isPublic{|key|

		var publicKey = this.formatKey(key);
		^ shared.includesKey(publicKey);
	}


	property_ {|key, ival, publish = true, changer, mutable=true, guitype, silent = false|

		var shared, property;

		publish = publish ? true;
		mutable = mutable ? true;
		silent = silent ? false;

		this.prValidID(key).not.if({
			MooError("Property must start with a letter and not contain special characters").throw;
		});

		key = key.asSymbol;

		"property: %".format(this.formatKey(key)).debug(this.name);

		//"property_ % %".format(key, ival).debug(this.class);

		//"verbs %".format(verbs).debug(this);

		verbs.includesKey(key).if({
			MooError("% name already in use by %".format(key, this.name)).throw;
		});

		mutable.not.if({
			guitype = guitype ? \none
		});


		//"properties %".format(properties).debug(this);

		properties.includesKey(key).if({
			// overwrite. Send notification
			//"overwrite %".format(key).debug(this.id);
			shared = properties.at(key);
			silent.if({
				shared.silentValue_(ival, changer);
			} , {
				shared.value_(ival, changer);
			});
		}, {

			publish.if({
				shared = this.addShared(this.formatKey(key), ival, owned:false, silent:silent);
				//shared = this.addRemote(this.formatKey(key));//, ival);
				//shared.value_(ival, moo);
				//api.add("property/%".format(this.id).asSymbol,
				//{arg key, ival, publish = true, changer, mutable=true, guitype, origin;
				//api.sendMsg("property/%".format(this.id).asSymbol, key, ival,
				//	publish, changer, mutable, guitype, moo.api.nick);
			} , {
				shared = this.addLocal(this.formatKey(key), ival);
			});
			shared.guitype = guitype ? shared.guitype;
			shared.silent = silent;

			property = MooProperty(shared, mutable);

			properties.put(key, property);
			this.put(key, shared); // make sure it's accessible with out the ID

			//advertise {|property, key, publish, guitype|
			silent.not.if({
				"not silent".debug(this.id);
				this.advertise(property, key, publish, shared.guitype, silent);  //advertise {|property, key, publish|
				//api.remote_query;
			});

			property.action_(moo.api, {|prop|
				"!!!action!!!!".debug(property);
				moo.api.sendMsg(this.formatKey(key), prop.value, moo.api.nick)
			});

			moo.api.add(this.formatKey(key), {|val, nick|
				var oldSym = false, newSym = false, changed = false, prop = property.value;

				"OSC input % % %".format(val, nick).debug(property);
				(nick != moo.api.nick).if({
					"not me".debug(property);
					(property.value != val).if({
						val.isKindOf(Symbol).if({
							val = val.toString;
							oldSym = true;
						});
						prop.isKindOf(Symbol).if({
							newSym = true;
							prop = prop.toString;
						});
						((val.isKindOf(String)) && (prop.isKindOf(String))).if({
							changed = (val.compare(prop) != 0); // if 0, unchanged
						} , {
							changed = (prop != val);
						});

						changed.if({
							//property.silentValue_(val, moo.api);
							property.value_(val, moo.api);
						});
					})
				});
			});

			// covered by advertise above
			//moo.api.sendMsg("property/%".format(this.id).asSymbol,
			//	key, property.value, publish,  moo.api.nick,  mutable, guitype, moo.api.nick);


		});
		/*
		api.add("property/%".format(this.id).asSymbol,
		{arg key, ival, publish = true, changer, mutable=true, guitype, origin;
		(origin != moo.api.nick).if({
		this.property_(key, ival, publish = true, changer, mutable=true, guitype);
		});
		});
		*/

		//properties.keys.postln;
		//"saved as % & % & %".format(properties[key].value, shared, this.perform(key).value).debug(this.id);
		this.name;
		//properties.debug(this.id);

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
		//this.prValidID(key).not.if({
		MooVerb.validID(key).not.if({
			MooError("Verbs must start with a letter and not cotnain special characters").throw;
		});


		//"New verb %".format(key).postln;

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
		var verb, property, func, ret;

		//"doesNotUnderstand %".format(selector).debug(this.id);

		//this.dumpBackTrace;

		selector = selector.asSymbol;

		if(this.respondsTo(selector), {
			^this.perform(selector, *args);
		});

		// first try moo stuff
		verb = this.getVerb(selector);//verbs[selector];
		if (verb.notNil) {

			//^func.functionPerformList(\value, this, args);
			^verb.invoke(args[0], args[1], args[2], args[3]);
		};

		//"% is not a verb".format(selector).debug(this.id);
		//verbs.keys.postln;

		if (selector.isSetter) {
			selector = selector.asGetter;
			if(this.respondsTo(selector), {
				(selector == \me).if({ Error("me!!").throw });
				warn(selector.asCompileString
					+ "exists as a method name, so you can't use it as a pseudo-method.");
				//this.dumpBackTrace;
				{
					this.perform(selector.asSetter, *args);
					//"new result is %".format(this.perform(selector)).debug(this.id);
				}.try({|err| err.postln; });
			}, {
				//"setter".debug(this.id);
				property = this.property(selector);
				property.notNil.if({
					//^(properties[selector].value_(*args));
					// does this belong to us?
					properties[selector].notNil.if({
						ret = (properties[selector].value_(*args));
						//^ret;
					} , {
						// it must belong to a parent
						// we need a way of seeing if it's shared or not
						this.property_(selector, *args);
						//^ret;
					});
					this.update;
					^ret;
				});
			});
		};

		property = this.property(selector);//properties[selector];
		property.notNil.if({
			//"porperty % %".format(selector, property.value).debug(this.id);
			^property.value;
		});

		//"not a property".debug(this.id);
		//properties.keys.postln;

		^nil;

	}

	getClock {|quant|
		var clock, loc;
		clock = this.property(\clock).value;
		clock.isKindOf(MooClock).if({
			quant = quant ? clock.quant;
			^[clock.clock, quant];
		});

		//"we don't have a clock".debug(this.name);

		loc = this.location;

		//"loc %".format(loc).debug(this.name);

		loc.notNil.if({
			^loc.getClock(quant);
		});

		^nil
	}


	move {|newLocation, caller, oldlocation|

		var oldLocation, moved = false, rectify, locClass, oldId= -1, doMsg = false;

		rectify = {|loc|

			{

				"recity loc %".format(loc).debug(this.name);

				loc.isKindOf(Symbol).if({
					loc = loc.asString;
				});
				loc.isKindOf(String).if({
					loc.isDecimal.if({
						loc = loc.asInteger;
					});
				});

				loc = MooObject.mooObject(loc, moo);

				loc.isNil.if({
					loc = -1;
				});

				"loc is %".format(loc).debug(this.name);
			}.try({|err| err.warn });

			loc
		};

		"new locastion was %".format(newLocation).debug(this.id);


		oldLocation = oldlocation ? this.location;

		oldLocation = rectify.value(oldLocation);
		newLocation = rectify.value(newLocation);

		"new locastion is %".format(newLocation).debug(this.id);

		//(oldLocation != newLocation).if({


		this.isPlayer.not.if({
			immobel.not.if({
				// not a player - need a container
				moved = newLocation.isKindOf(MooContainer).if({ newLocation.addObject(this, caller) });
				((oldLocation != newLocation) && moved ).if({
					oldLocation.isKindOf(MooContainer).if({
						oldLocation.remove(this, caller, false);
						oldId = oldLocation.id;
					});

					"moved %".format(this.name).debug("MooObject.move");
					doMsg = true;


				});

				//moved = true;
			});
		} , {
			// are a player - need a room
			"% is a player".format(this.name).debug("MooObject.move");

			newLocation.isKindOf(MooRoom).if({

				"% is a room".format(newLocation.name).debug("MooObject.move");
				//name = this.property(\name);
				//oldLocation.player.remove(this);

				(this.me &&  (oldLocation != newLocation)).if({

					oldLocation.isKindOf(MooRoom).if({
						oldLocation.depart(this, oldLocation, this, oldLocation);
						oldId = oldLocation.id;
					});
					// added arrive on Train
					newLocation.arrive(this, newLocation, this, newLocation);
					moved = true;

					// just tell people to move this player
					doMsg = true;
					//moo.api.sendMsg ("move/%".format(this.id).asSymbol, newLocation.id, oldId, moo.api.nick);

				} , {
					// just move quietly
					(oldLocation != newLocation).if({
						oldLocation.isKindOf(MooRoom).if({ oldLocation.removePlayer(this); });
					});
					moved = newLocation.addPlayer(this);
				});

				//moved = true;
			});
		});
		//});

		moved.if({ "move of % happened".format(this.name).debug("MooObject.move") },
			{ "move of % failed".format(this.name).debug("MooObject.move") });

		moved.if({
			caller = caller ? moo.me;

			//{ this.property(\location).silence = false; }.try; //this is not reliably containerised
			{ properties[\location].silence = false; }.try;
			this.location_(newLocation, caller);
			doMsg.if({
				moo.api.sendMsg ("move/%".format(this.id).asSymbol, newLocation.id, oldId, moo.api.nick);
			});
		})
	}


	matches{|key|

		var matches = false;

		key = key.asString;

		//"this.name %".format(this.name).debug(this);

		//matches = (key == this.name);

		matches = ( key.compare(this.name.value.asString, true) == 0 );

		matches.not.if({

			//matches = aliases.includes(key)
			//matches = aliases.includesIgnoreCase(key)
			matches = aliases.any({|alias|
				key.compare(alias.asString, true) == 0;
			});
		});

		//"matches %".format(matches).debug(this);

		^matches;
	}


	== {|other|

		//other.debug(this.name);
		//other = MooObject.mooObject(other, moo);
		//other.debug(this.name);
		other.isKindOf(MooObject).if({
			^(other.id == this.id);
		});

		^false;
	}

	!= {|other|
		^((this == other).not)
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

		var public, str="", synths ="", props, prop, mutable, json_verbs, ownerID; //properties.collect({|p| converter.convertToJSON(p) });



		props = properties.keys.collect({|key|
			public = this.isPublic(key);
			mutable = "";
			prop = properties[key];
			prop.respondsTo(\mutable).if({
				mutable = ", \"mutable\" : %".format(prop.mutable);
			});

			"{ \"%\" : %, \"public\" : % % }".format(key, converter.convertToJSON(prop), public, mutable);
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

		ownerID = owner;
		ownerID.isKindOf(MooObject).if({
			ownerID = ownerID.id;
		});

		//"id %".format(this.id.asString).debug(this);

		str = "\"id\" : \"%\", ".format(this.id.asString);
		str = str + "\"class\" : \"%\",".format(this.class);
		str = str + "\"name\" : \"%\", ".format(this.name);
		str = str + "\"verbs\" : [ % ],".format(json_verbs.join(", "));
		str = str + "\"properties\" : [ % ],".format(props.join(", ")); //format(converter.convertToJSON(props)) ;
		str = str + "\"aliases\" : %,".format(converter.convertToJSON(aliases)) ;
		//str = str + "\"location\" : %,".format(location !? { converter.convertToJSON(location.id) } ? "null");
		str = str + "\"location\" : %,".format(this.location !? { converter.convertToJSON(this.location.id) } ? "null");
		str = str + "\"immobel\" : %,".format(immobel.asCompileString) ;
		str = str + synths;
		str = str + "\"owner\": % ".format(owner !? {converter.convertToJSON(ownerID)} ? "null") ;

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

	*new { |moo, name, maker, stage, id, location|
		^super.new.initClock(moo, name, maker, stage, id, location);
	}

	initClock {| moo, name, maker, istage, id, location|

		var sharedTempo;

		super.initMooObj(moo, name, maker);

		stage = istage;

		istage.notNil.if({
			stage = istage;
			//this.location = (stage.location, maker);
			this.property_(\location, stage.location, changer: maker);
			this.location.add(this);
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

	*new { |moo, name, maker, parent, local, id, location|
		^super.new.initStage(moo, name, maker, location);
	}

	initStage {| moo, name, maker, location|

		super.initMooObj(moo, name, maker);
		players = [];
		speakers = [];
		//location = maker.location;
		this.location_(maker.location, maker);
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

	*new { |moo, name, maker, parent, local, id, location|
		"new MooContainer".debug(name);
		^super.new(moo, name, maker, parent ? this.generic(moo), local, id, location).initContainer();
	}

	*fromJSON{|dict, converter, moo|
		//"fromJSON MooContainer".debug(this);
		^super.fromJSON(dict, converter, moo).containerRestore(dict, converter, moo);
	}


	initContainer{

		semaphore = Semaphore(1);
		contents = [];

	}

	remove {|item, caller, shouldBlock=true, notify=false|

		var key, removedItem, found;

		//"remove %".format(item).debug(this.class);
		//"remove sempahore wait".debug(this.name);

		shouldBlock.if({
			semaphore.wait;
		});

		//"remove waited".debug(this.name);

		removedItem = contents.remove(item);

		//"removed from contents".debug(this.class);

		//playableEnv.remove(item);
		key = this.findKeyForValue(item);
		//"key %".format(key).debug(this.class);
		//super.remove(item);

		key.isNil.if({
			//this.findObj(item).if({
			key = item.asString.asSymbol;
			//});
		});

		key.notNil.if({
			found = this.removeAt(key);
			(removedItem.isNil && found.notNil).if({
				removedItem = contents.remove(found);
				removedItem = removedItem ? found;
			});
			//"removed from environment".debug(this.class);
		});

		notify.if({
			removedItem.notNil.if({
				(removedItem.location == this).if({
					removedItem.location_(nil, caller);
				});
			});
		});

		shouldBlock.if({
			semaphore.signal;
		});

		//"remove signaled".debug(this.name);

		// return the item
		^removedItem;

	}

	addObject {|item, caller, shouldBlock=true|

		var old_location, mobile = item.immobel.not, removed = true;

		// check if the item can move

		"addObject".debug(this.name);

		item.immobel.if({
			caller.isKindOf(MooPlayer).if({
				((caller == item.owner) || caller.wizard).if({
					mobile = true;
				});
			});
		});

		mobile.if({

			old_location = item.location;

			(old_location != this).if({

				"addObject wait".debug(this.name);
				shouldBlock.if({
					semaphore.wait;
				});

				"addObject waited".debug(this.name);

				old_location.notNil.if({
					removed = old_location.remove(item, caller);
				});

				//removed.notNil.if({
				contents.includes(item).not.if({
					contents = contents.add(item);
					//playableEnv.put(item.name.asSymbol, item);
					this.put(item.name.asSymbol, item);

					item.location_(this, moo, caller);
				});
				//});

				shouldBlock.if({
					semaphore.signal;
				});
				//"addObject signaled".debug(this.name);
			} , {
				// old_location is this
				"addObject wait".debug(this.name);
				shouldBlock.if({
					semaphore.wait;
				});

				contents.includes(item).not.if({
					// we don't actually have the thing
					contents = contents.add(item);
					//playableEnv.put(item.name.asSymbol, item);
					this.put(item.name.asSymbol, item);

					item.location_(this, moo, caller);
				});

				shouldBlock.if({
					semaphore.signal;
				});


			});
		});

		// return success or failure
		^mobile;


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

		//"containerRestore".debug(this);

		semaphore = semaphore ? Semaphore(1);
		contents = [];


		json_contents = dict.atIgnoreCase("contents");
		//"contents %".format(contents).debug(this);
		contents = contents ++ json_contents.collect({|item| this.class.refToObject(item, converter, moo) });
		contents = contents.as(Set).as(Array); // dedup // thanks dkg
	}


	restored {
		super.restored;

		//"restore wait".debug(this.name);
		semaphore.wait;
		//"restore waited".debug(this.name);
		contents = contents.collect({|item|

			// in case we just have an ID
			item = this.class.mooObject(item, moo);
			//"taking names %".format(item.name).debug(this);

			this.put(item.name.asSymbol, item);
			item;
		});
		contents = contents.as(Set).as(Array); // dedup // thanks dkg
		semaphore.signal;
		//"restore signaled".denug(this.name);

	}



}

MooRoom : MooContainer {

	var <mooPlayers, <exits;

	*new { |moo, name, maker, parent, local, id|

		^super.new(moo, name, maker, parent ? this.generic(moo), local, id).initRoom();
	}

	*fromJSON{|dict, converter, moo|
		//"fromJSON MooRoom".debug(this);
		^super.fromJSON(dict, converter, moo).roomRestore(dict, converter, moo);
	}

	roomRestore{ |dict, converter, moo|

		var json_exits, key, value;

		//"roomRestore".debug(this);

		//semaphore = Semaphore(1);
		mooPlayers = [];
		//contents = [];
		exits = IdentityDictionary();

		//departures = exits.keysValuesDo({|key, val| "{ \"key\": \"%\", \"val\": %}".format(key, val !? {val.id} ? "null") }).asList;

		json_exits = dict.atIgnoreCase("exits");
		json_exits.do({|item|
			key = item.atIgnoreCase("key");
			value = item.atIgnoreCase("val");
			value = MooObject.refToObject(value, converter, moo);
			exits.put(key.asSymbol, value);
		});
	}

	restored {
		super.restored;

		//"restored wait".debug(this.name);
		semaphore.wait;
		//"restored waited".debug(this.name);
		exits = exits.collect({|item|

			// in case we just have an ID
			item = this.class.mooObject(item, moo);
			//"taking names %".format(item.name).debug(this);

			//this.put(item.name.asSymbol, item);
			item;
		});
		semaphore.signal;
		//"restored signal".debug(this.name);

	}



	initRoom {

		//super.initMooObj(true, moo, name, maker);
		mooPlayers = [];
		aliases = ["here"];
		exits = IdentityDictionary();
		immobel = true;


	}

	announce {|str, caller|

		var tell;

		//"anounce %".format(str).debug(this.name);

		caller = caller ? moo.me;

		mooPlayers.do({|player|
			"paleyer %".format(player.name).debug(this.name);
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

		caller = caller ? moo.me;

		excluded.isKindOf(MooObject).if({
			excluded = [excluded];
		});

		//excluded.debug(this.name);

		mooPlayers.do({|player|
			//"player".debug(this.name);
			(excluded.includes(player)).not.if({ // if the excluded list does NOT container the player
				//"post".debug(this.name);
				player.postUser(str, caller);
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
		//"removePlayer wait".debug(this.name);
		semaphore.wait;
		//"removePlayer.waited".debug(this.name);
		//players.size.debug(this.name);
		"Remove %".format(player.name).debug(this.name);

		mooPlayers.remove(player);

		players.debug(this.name);
		//playableEnv.remove(player);
		this.remove(player, player, false);
		semaphore.signal;
		//"removePlayer.signaled".debug(this.name)
	}

	addPlayer{|player|
		//"addPlayer".debug(this.name);
		//semaphore.dump;

		semaphore.wait;

		mooPlayers.isNil.if({
			mooPlayers = [];
		});

		//"add Playwe waited".debug(this.name);
		mooPlayers.includes(player).not.if({
			mooPlayers = mooPlayers.add(player);
		});
		//"players.add(player done".debug(this.name);
		//playableEnv.put(player.name.asSymbol, player);
		this.put(player.name.asSymbol, player);
		//"this.put(player.name.asSymbol, player); done".debug(this.name);
		semaphore.signal;
		//"addPlayer signaled".debug(this.name);

		^true; // return success
	}

	exit{|key|
		^MooObject.mooObject(exits.atIgnoreCase(key) , moo);
	}

	addExit{|key, room|

		room = MooObject.mooObject(room);
		room.isKindOf(MooRoom).not.if({
			MooTypeError("% is not a MooRoom".format(room)).throw;
		});

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
			found = mooPlayers.detect({|obj| obj.matches(key) });
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

		var stuff, departures, val;

		//"exits %".format(exits.keys).debug(this);

		//stuff = contents.collect({|c| c !? { c.id } ? "null" });
		departures = exits.keys.collect({|key|
			val = exits.at(key);
			// if val is not null, try getting the id, but if that fails, just use val
			"{ \"key\": \"%\", \"val\": %}".format(key, val !? {val.id}.try(val) ? "null")
		}).asList.join(", ");

		//"departures %".format(departures).debug(this);

		^super.pr_JSONContents(converter) +
		//",\"contents\":  % ," .format(converter.convertToJSON(stuff)/*stuff.join(", ")*/) +
		", \"exits\": [ % ]".format(departures);//.format(converter.convertToJSON(departures)/*departures.join(",\n")*/);
	}




}
