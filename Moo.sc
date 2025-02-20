Moo {
	var index, objects, <netAPI, semaphore, <pronouns;

	*new{|netAPI, json|
		^super.new.init(netAPI)
	}

	init {|net, json|
		netAPI = net ? NetAPI.default;

		semaphore = Semaphore(1);
		//pronouns = IdentityDictionary[ \masc -> IdentityDictionary[
		//	\sub -> "He", \ob-> "Him", \pa-> "His", \po -> "His", \ref -> "Himself" ];
		//];

		objects = [];
		index = 0;
		/*
		json.isNil.if({
			//MooObject(this, "dummy");
			//MooPlayer(this, "Root");
			//objects = [MooRoom(this, "Lobby", objects[0])];
			//index = 2;
		}, {
			// There needs to be a way to load this from JSON objects
		});
		*/
	}

	add { |obj|

		var obj_index;

		semaphore.wait;

		objects = objects.add(obj);
		obj_index = objects.size;
		index = index + 1;

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

}

MooError : Error {}

MooObject : NetworkGui {

	var <moo,  <>name, <owner, <id, <aliases, <>desc, verbs, <location,  <properties, <>immobel;


	*new { |moo, name, maker|

		^super.new.init(moo, name, maker);
	}

	init {|imoo, iname, maker|

		moo = imoo;
		owner = maker;

		super.make_init(moo.netAPI, nil, {});

		id = moo.add(this);
		name = iname ? id.asString;
		desc = "";
		aliases = [];
		verbs = IdentityDictionary();
		properties = IdentityDictionary();
		immobel = false;
	}


	decribe {| str|

		desc = str;
	}

	alias {|new_alias|

		aliases = aliases.add(new_alias);
	}

	look {
		^desc
	}

	property_ {|key, ival, publish = true|

		var shared;

		key = key.asSymbol;

		((properties.includesKey(key)) || verbs.inclidesKey(key)).if({
			MooError("% name already in use by %".format(key, name)).throw;
		}, {

			shared = SharedResource(ival);
			properties.put(key, shared);
			publish.if({
				this.addShared("%/%".format(key, id).asSymbol, shared);
			});
		});

		^shared;
	}

	verb_ {|key, dobj, iobj, func, publish=false|

		var newV;

		key = key.asSymbol;

		((properties.includesKey(key)) || verbs.inclidesKey(key)).if({
			MooError("% name already in use by %".format(key, name)).throw;
		}, {
			newV = MooVerb(key, dobj, iobj, func, this, publish);

			verbs.put(key, newV);
		});
	}

	getVerb {|key|

		^verbs.at(key.asSymbol)
	}

	isPlayer{ ^false }
}


MooPlayer : MooObject {

	var contents, ownedObjects;

	*new { |moo, name|
		var player = super.new(moo, name);
		^player.init(moo, name);
	}

	init {|imoo, iname|

		var pronouns;

		super.init(imoo, iname, this);

		//owner = this;
		immobel = true;
		contents = [];
		ownedObjects = [];
		//pronouns = moo.pronouns.keys.choose;
		//pronouns = this.property_(\pronouns, moo.pronouns.keys.choose, false);

		/*this.verb_(\set_pronoun, \this, \any,  {|dobj, iobj, caller|

			iobj = iobj.asSymbol;

			(caller == this).if({ // no force femming
				moo.pronouns.includesKey(iobj).if({
					pronouns.value = pronouns;
				}, {
					MooError("Pronouns % not yet specified".format(iobj)).throw;
				});
			}); //uses a property so doesn't need to be published
		});*/
	}


	isPlayer{ ^true }


}


MooVerb{

	var <verb, <dobj, <iobj, <func, <obj;

	*new {|verb, dobj, iobj, func, obj, publish|

		^super.newCopyArgs(verb, dobj, iobj, func, obj).init(publish);
	}

	init{ arg publish = false;

		publish.if({
			this.publish;
		})
	}

	publish {
		// add to the api
		obj.moo.netAPI.add("%/%".format(verb, obj.id).asSymbol, {arg dob, iob; func(dob, iob)},
			"% % % % on %".format(verb, dobj, iobj, obj.name));
	}
}





MooClock : MooObject {

	var <stage, <clock;

	*new { |moo, name, maker, stage|
		var clock = super.new(moo, name, maker);
		^clock.init(moo, name, maker, stage);
	}

	init {| moo, name, maker, istage|

		var sharedTempo;

		super.init(moo, name, maker);

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

		this.verb_(\set, \this, \any,  {|dobj, iobj, caller|

			sharedTempo.value = iobj.asFloat;
		}); //uses a property so doesn't need to be published

		this.verb_(\play, \this, \any, {|dobj, iobj, caller|

			clock.play
		}, true);

		this.verb_(\stop, \this, \any, {|dobj, iob, caller|

			clock.stop
		}, true);

		desc = "The clock attached to %".format(stage.name);


	}


}

MooStage : MooObject {

	var players, clock, speakers;

	*new { |moo, name, maker|
		var stage = super.new(moo,name, maker);
		^stage.init(moo, name, maker);
	}

	init {| moo, name, maker|

		super.init(moo, name, maker);
		players = [];
		speakers = [];
		location = maker.location;
		immobel = true;

		clock = MooClock(moo, "%_clock".format(name), maker);

		verb_(\add, \any, \this,  {|dobj, iobj, caller|

			(caller == owner).if({
				dobj.isKindOf(MooPlayer).if({
					players.includes(dobj).not ({
						players = players.add(dobj);
					})
				})
			})
		});




	}




}



MooRoom : MooObject {

	var contents, players;

		*new { |moo, name, maker|

		^super.new.init(moo, name, maker);
	}

	init {| moo, name, maker|

		super.init(moo, name, maker);
		players = [];
		contents = [];
	}

}



MooParser {

	var actor, input, verb, dobj, preposition, iobj;

	init{| speaker, string|

		var tokenised;

		actor = speaker;
		input = string;

		this.parse();

		this.call();
	}

	call {

		var d_obj, i_obj, vfunc;

		d_obj = this.findObj(dobj);
		iobj.notNil.if({ i_obj = this.findObj(i_obj) });

		vfunc = checkObj(d_obj);
		vfunc.isNil.if({
			i_obj.notNil.if({
				vfunc = checkObj(i_obj);
			})
		});

		vfunc.notNil.if({ // call it!!!

			vfunc.func.value(d_obj, i_obj, actor);

		});
	}



	identifyStrings {

		var arr=[], word="", inString=false, seperator=$\".ascii;

		input.do({ arg let, i;
			(let.acii  == seperator).if({
				inString.if({
					word = word ++"\"";
					(word.size > 0).if({
						arr = arr.add(word);
						word = "";
					});
				} , {
					(word.size > 0).if({
						arr = arr.add(word);
						word = "\"";
					});
				});

				inString = inString.not;

			} , {
				word=word++let;
			});
		});
		(word.size > 0).if({
			arr = arr.add(word);
		});

		^arr;
	}

	parse {

		var deStringed, tokens;

		deStringed = this.identifyStrings().collect({|str| str.stripWhiteSpace });

		tokens = deStringed.collect({|str|
			(str.beginsWith("\"") || str.endsWith("\"")).not.if({
				str.split($ );
			}, {
				str;
			})
		}).flat;

		verb = tokens[0];
		dobj = tokens[1];

		(tokens.size > 2).if ({ iobj = tokens.last; });


	}



	checkObj { |obj |

		var storedVerb;

		storedVerb = obj.getVerb(verb);

		^storedVerb;
	}

	matchObj {| key, item|

		var found = false;

		(item.name.asSymbol == key).if ({

			found = true;  //found!

		}, {

			item.aliases.do({|alias|

				(alias.asSymbol == key).if ({

					found = true;
				});
			});
		});

		^found;
	}


	findObj { |key|

		var obj, found = false;

		key = key.asSymbol;

		actor.contents.do({|item|

			found = matchObj(key, item);
			obj = item;
		});

		found.not.if ({
			actor.location.players.do({|item|

			found = matchObj(key, item);
			obj = item;
		});
		});
		found.not.if ({actor.location.contents.do({|item|

			found = matchObj(key, item);
			obj = item;
		});
		});

		found.not.if.({ obj = key });

		^obj


	}





}