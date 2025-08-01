

MooParser {

	classvar <reservedWords, >i18n;
	var actor, input, verb, dobj, preposition, iobj, dobj_object, iobj_object, li18n;


	* initClass{

		reservedWords = IdentityDictionary();
		//i18n = MooI18n();//IdentityDictionary();
	}

	*i18n {
		i18n.isNil.if({
			i18n = Moo.localisation;
		});
		^i18n;
	}

	*addI18Pair{|key, val|

		//i18n[key] = val;
		// if we're looking up symbols, make it bidirectional
		//val.isKindOf(Symbol).if({
		//	i18n[val] = key;
		//})
		this.i18n.addPair(key, val);
	}

	*reserveWord{|key, object| // optionally tie the word to a thing

		reservedWords = reservedWords.put(key.asSymbol, object);

	}

	*reservedWord{|key|
		var result;
		result = reservedWords.at(key.asSymbol);
		result.isNil.if({
			reservedWords.atIgnoreCase(key);
		});

		^result;
	}

	*toJSON{|converter|
		^converter.convertToJSON(reservedWords);
	}

	*fromJSON{|converter, moo, dict|

		var item;

		dict.keys.do({|key|

			item = converter.restoreMoo(dict.at(key), moo);
			//"item %".format(item).debug(this);

			this.reserveWord(key.asSymbol, item);
		});
	}


	toJSON{|converter|
		^this.class.toJSON(converter);
	}

	reserveWord{|key, object|
		^this.class.reserveWord(key, object);
	}

	reservedWord{|key|
		^this.class.reservedWord(key);
	}


	*new {|player, string|
		^super.new.init(player, string);
	}

	init{| speaker, string|

		var tokenised;
		li18n = this.class.i18n;

		actor = speaker;
		input = string;

		//this.parse();
		this.readyParse();

		//"parsed".postln;

		//"actor % input %".format(actor, input)

		//"location % ".format(speaker.location).debug("MooParser init");

		//actor.name.debug(this);

		this.movement().not.if({
			this.creation().not.if({
				this.call();
			});
		});
	}

	movement {

		var place, matched = false;

		//"movement".debug(this);

		dobj.isNil.if({
			actor.location.notNil.if({
				place = actor.location.exit(verb);
				place.notNil.if({
					//"move".debug(this);
					{ actor.move(place, actor); }.fork;
					matched = true
				});
			});
		});
		^matched;
	}

	creation {

		var thing, matched = false, index, switch, clone;

		//"creation".debug(this);

		(verb.asString.toLower.asSymbol == li18n[\make]).if({

			dobj.notNil.if({



				switch = {|key, obj|

					matched = true;

					switch(key,
						li18n[\room], {
							thing = MooRoom(actor.moo, obj, actor, nil, true);
							//actor.postUser("Here (%) is object number %\nNew room % is object number %".format(actor.location.name, actor.location.id, thing.name, thing.id), actor);
							//{ actor.move(thing); }.fork;
							//actor.postUser(li18n[\newRoomMsg].value(actor.location.name, actor.location.id, thing.name, thing.id), actor);
							actor.postUser(li18n.format(\newRoomMsg, actor.location.name, actor.location.id, thing.name, thing.id), actor);
						},
						li18n[\stage], {
							thing = MooStage(actor.moo, obj, actor, nil, true);
							thing.location = actor.location;
							{ actor.location.addObject(thing); }.fork;
						},
						li18n[\clock], {
							thing = MooClock(actor.moo, obj, actor, nil, true);
							//thing.location = actor.location;
							{ actor.addObject(thing); }.fork;
						},
						li18n[\object], {
							thing = MooObject(actor.moo, obj, actor, nil, true);
							{ actor.addObject(thing); }.fork;
						},
						li18n[\container], {
							thing = MooContainer(actor.moo, obj, actor, nil, true);
							{ actor.addObject(thing); }.fork;
						},
						{ matched = false; }
					);

					matched;
				};

				matched = switch.(dobj.toLower.asSymbol, iobj);
				//"matched %".format(matched).debug(this);
				matched.not.if({
					iobj.isNil.if({
						matched = switch.(\object, dobj.asSymbol);
						matched.if({iobj = dobj});
					});
				});

				matched.if({
					//actor.postUser("Made %.".format(iobj), actor);
					//actor.postUser(li18n[\newObjMsg].value(iobj), actor);
					actor.postUser(li18n.format(\newObjMsg, iobj), actor);
				});
			});
		});


		(verb.asString.toLower.asSymbol == li18n[\exit]).if({
			(actor.location.owner == actor).if({
				// exit north to 135
				(iobj.isDecimal).if({
					index= iobj.select({|c| c.isDecDigit }).asInteger;
					//index = iobj.stripWhiteSpace.asInteger;
					//"% % iobj is % index is".format(iobj.class, iobj.isDecimal, iobj, index).debug(this);
					(index > 0).if({
						thing = actor.moo[index];
					});
				});
				thing.isNil.if({
					thing = this.findObj(iobj);
				});
				thing.isNil.if({
					// not a number. Make a new room and connect it
					thing = MooRoom(actor.moo, iobj, actor);
					actor.postUser("Here (%) is object number %".format(actor.location.name, actor.location.id), actor);
					actor.postUser("New room % is object number %".format(thing.name, thing.id), actor);
				});

				actor.location.addExit(dobj, thing);
				actor.postUser("New exit % to %".format(dobj, thing.name), actor);
				matched = true;
			});
		});

		(verb.asString.toLower.asSymbol == \copy).if({

			//"copy".debug(this);

			(dobj.isDecimal).if({
				index= dobj.select({|c| c.isDecDigit }).asInteger;
				//index = iobj.stripWhiteSpace.asInteger;
				//"% % dobj is % index is".format(dobj.class, dobj.isDecimal, dobj, index).debug(this);
				(index > 0).if({
					clone = actor.moo[index];
				});
			});
			clone.isNil.if({
				clone = this.findObj(dobj);
			});
			clone.notNil.if({
				matched = true;
				thing = clone.class.new(actor.moo, iobj.asSymbol, actor, clone, true);
				{ actor.addObject(thing); }.fork;
			});
		});


		^matched;
	}

	call {

		var d_obj, i_obj, vfunc, found, object, sub_verb, called=false;

		//"verb: %".format(verb).postln;

		// try to match the dobj to an object
		dobj.notNil.if({
			this.isString(dobj).if({
				d_obj = dobj;
			} , {
				d_obj = this.findObj(dobj);

				d_obj.isNil.if({
					d_obj = this.reservedWord(dobj);
				});
			});
			//"d_obj %".format(d_obj).debug(this);
		});

		// try to match the iobj to an object
		iobj.notNil.if({
			this.isString(iobj).if({
				i_obj = iobj;
			} , {
				i_obj = this.findObj(iobj);

				i_obj.isNil.if({
					i_obj = this.reservedWord(iobj);
				});
			});
			//"i_obj %".format(i_obj).debug(this);
		});

		// now try to find the verb
		// is this a verb on the direct object?
		d_obj .notNil.if({
			vfunc = this.checkObj(d_obj);
			object = d_obj;
		});


		vfunc.isNil.if({
			i_obj.notNil.if({ // perhaps it's on the indirect object?
				vfunc = this.checkObj(i_obj);
				object = i_obj;
			})
		});

		// see if it's on the room
		vfunc.isNil.if({
			vfunc = this.checkObj(actor.location);
			vfunc.notNil.if({

				object = actor.location;

				/*
				d_obj.isNil.if({
				d_obj = actor.location;

				},{
				i_obj.isNil.if({
				i_obj = actor.location
				});
				});
				*/

			});
		});

		// is it on the caller?
		vfunc.isNil.if({
			vfunc = this.checkObj(actor);
			vfunc.notNil.if({

				object = actor;

				/*
				d_obj.isNil.if({
				d_obj = actor
				},{
				i_obj.isNil.if({
				i_obj = actor
				});
				});
				*/

			});
		});

		// replace nils with the passed in strings/keys
		d_obj.isNil.if({
			d_obj = dobj;
		});
		i_obj.isNil.if({
			i_obj = iobj;
		});


		vfunc.notNil.if({ // call it!!!

			//"found vfunc".debug(this);
			//vfunc.func.value(d_obj, i_obj, actor);
			vfunc.invoke(d_obj, i_obj, actor, object);
			^true;

		}, {
			//"not invoked".debug(this);
			sub_verb = this.reservedWord(verb);
			//"found % , a %".format(sub_verb, sub_verb.class).debug(this);

			(sub_verb.isKindOf(String) || sub_verb.isKindOf(Symbol)).if({
				//"found a reserve".debug(this);
				verb = sub_verb;
				called = this.call;
			});
		});

		called.not.if({
			actor.postUser("Input not understood.", actor);
		});
		^called;
	}



	identifyStrings {

		// this is meant to find strings which are deliniated with doble qoutes: \"

		var arr=[], word="", inString=false, seperator=$\".ascii;

		input.do({ arg let, i;
			(let.ascii  == seperator).if({
				inString.if({
					word = word ++"\"";
					(word.size > 0).if({
						arr = arr.add(word);
						word = "";
					});
				} , {
					(word.size > 0).if({
						arr = arr.add(word);
						//word = "\"";
					});
					word = "\"";
					//"started a string".debug(this)
				});

				inString = inString.not;

			} , {
				word=word++let;
			});
		});
		(word.size > 0).if({
			arr = arr.add(word);
		});

		//arr.postln;

		^arr;
	}

	isString {|token|

		//"isString".debug(this);

		^(token.beginsWith(""++i18n[\openString]) && token.endsWith(""++i18n[\closeString]));

	}

	stripQuotes{|token|
		token.endsWith(""++i18n[\closeString]).if({
			token = token[0..(token.size-2)];
		});
		token.beginsWith(""++i18n[\openString]).if({
			token = token[1..];
		});

		^token
	}

	pr_arrFlat{|arr, count|

		var assembled = [];

		//[arr, count].postln;

		arr.do({|item|
			item.isKindOf(String).not.if({
				assembled = assembled ++ this.pr_arrFlat(item, count +1);
			}, {
				assembled = assembled.add(item);
			});
		});

		^assembled;
	}


	specialCase{

		var found = false;

		// check for a " say
		((input[0] == $\") && input.copyRange(1, input.size-1).includes($\").not).if({

			// there is exactly one quote at the start of the line
			verb = \say;
			dobj = input++"\"";
			found = true;
		});

		// check for a / pose
		(found.not && (input[0] == $\/)).if({

			verb = \pose;
			dobj = "\"" ++ input.copyRange(1, input.size-1).stripWhiteSpace ++ "\"";
			found = true;
		});

		^found;
	}

	readyParse {
		var deStringed, tokens;

		this.specialCase.not.if({

			deStringed = this.identifyStrings().collect({|str| str.stripWhiteSpace });

			tokens = deStringed.collect({|str|
				/*
				(str.beginsWith("\"") || str.endsWith("\"")).not.if({
				str.split($ );
				}, {
				str;
				})
				*/
				this.isString(str).if({
					str;
				} , {
					str.split($ );
				});
			});

			tokens = this.pr_arrFlat(tokens, 0);

			this.parse(tokens);
		});
	}




	checkObj { |obj |

		var storedVerb;

		obj.isKindOf(MooObject).if({

			storedVerb = obj.getVerb(verb);
		});

		^storedVerb;
	}

	/*

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
	*/


	findObj { |key|

		var obj, found = false, num, str;

		obj = key;

		(key.isKindOf(String) || key.isKindOf(Symbol)).if({
			str = key.asString;
			str.beginsWith("\#").if({
				str = str[1..];
			});

			str.isDecimal.if({
				//"key %".format(key).debug(this);
				num = str.select({|c| c.isDecDigit }).asInteger;
				obj = actor.moo.at(num);
				obj.notNil.if({
					found = true;
				}, {
					obj = key; // nevermind
				});
			});
		});

		key.isKindOf(SimpleNumber).if({
			// it's the object ID
			obj = actor.moo.at(key);
			obj.notNil.if({
				found = true;
			}, {
				obj = key; // nevermind
			});
		});

		key = key.asSymbol;

		(found.not && (key ==\me)).if({
			//"me".debug(this);
			found = true;
			obj = actor;
		});

		found.not.if({
			(key ==\here).if({
				found = true;
				obj = actor.location;
			});
		});


		found.not.if({
			//actor.contents.do({|item|

			//	found = item.matches(key);
			//	obj = item;
			//});
			obj = actor.contents.detect({|item| found = item.matches(key); })
		});

		found.not.if ({
			//actor.location.players.do({|item|

			//found = matchObj(key, item);
			//	found = item.matches(key);
			//	obj = item;
			//});
			obj = actor.location.players.detect({|item| found = item.matches(key); })
		});
		found.not.if ({
			//actor.location.contents.do({|item|

			//found = matchObj(key, item);
			//	found = item.matches(key);
			//	obj = item;
			//});
			obj = actor.location.contents.detect({|item| found = item.matches(key); })
		});

		//found.not.if.({ obj = key });

		//"key is % object is %".format(key, obj).debug(this);

		^obj

	}

}






MooVerb{

	classvar <disallowed, <reserved;
	var verb, <dobj, <iobj, funcstr, <obj, func, owner, <published;

	*initClass {

		disallowed = ["unixCmd", "unixCmdGetStdOut", "systemCmd", "runInTerminal", "setenv",
			"unsetenv", "mkdir", "pathMatch", "load", "loadPaths", "File", "loadRelative",
			"resolveRelative", "standardizePath", "openOS", "basename", "dirname", "splittext",
			"parseYAML", "parseYAMLFile", "parseJSON", "parseJSONFile", "newTextWindow",
			"openDocument", "findHelpFile", "help", "unixCmdGetStdOutLines", "absolutePath2",
			"folderContents", "isFile", "isFolder", "pathExists", "makeDir", "renameTo",
			"copyTo", "copyFile", "copyToDir", "copyRename", "copyReplace", "replaceWith",
			"copyToDesktop", "create_scwork", "copyTo_scwork", "moveToDir", "moveTo",
			"moveRename", "moveReplace", "removeFile", "zip", "unzip", "tar", "untar", "gz",
			"ungz", "targz", "tgz", "untgz", "openWith", "openWithID","openInFinder",
			"showInFinder", "downloadURL", "loadPath", "loadDocument", "asPathName",
			"realNextName", "curl", "curlMsg", "dirLevel", "downloadCVSSource",
			"updateCVSSource", "downloadSVNSource", "getSubDirectories", "findSubDirectories",
			"openHTMLFile", "openServer", "revealInFinder", "perform", "performList",
			"performMsg", "performWithEnvir", "performKeyValuePairs", "tryPerform",
			"superPerform", "superPerformList", "multiChannelPerform", "Pipe", "UnixFILE"
		];

		reserved = ["public"];
	}

	*validID {|key|
		var naughty_count = 0, valid = true, chopped;

		key.isKindOf(Symbol).if({ key = key.asString });

		key.isKindOf(String).if({
			valid = MooVerb.pass(key);

			key.beginsWith("@").if ({ // this is allowed
				chopped = key[1..]
			} , {
				chopped = key
			});

			// weird chars
			naughty_count = chopped.sum({|char| (char.isAlphaNum).if({ 0 } , { 1 }) });
			(naughty_count > 0).if({ valid = false });

			// starts with a number
			chopped[0].isAlpha.not.if({ valid = false; });

			reserved.do({|naughty|
				(chopped.compare(naughty, true) == 0).if({
					valid = false;
				});
			});
		} , {
			// whatever else is happening here is weird and I don't like it
			valid = false;
		});

		^valid;
	}


	*pass {|str|
		var pass, problem_count;

		// this looks for subsctrings, so because "tar" is a naughty command, we can't have a tardis
		// obviously this needs fixing via a better regex

		problem_count = MooVerb.disallowed.collect({|naughty|
			str.find(naughty, true).isNil.if({0}, {1}) + // 1 for a match, 0 for no match
			str.find(naughty.reverse, true).isNil.if({0}, {1});
		}).sum;

		pass = (problem_count ==0);
		//pass.debug(this);
		^pass;
	}

	*fromJSON{|dict, converter, moo, object|

		var verb, dobj, iobj, func, obj, publish, owner, json_obj, id, a;
		//"{ \"verb\": \"%\", ".format(verb) +
		//"\"args\": [\"%\", \"%\"],".format(dobj, iobj) +
		//"\"func\": %, ".format(converter.convertToJSON(func)) +
		//"\"published\": %, ".format(converter.convertToJSON(published)) +
		//"\"owner\": % }" .format(converter.convertToJSON(owner));

		verb = dict.atIgnoreCase("verb");
		dobj = dict.atIgnoreCase("dobj");
		iobj = dict.atIgnoreCase("iobj");
		func = dict.atIgnoreCase("func");
		publish = dict.atIgnoreCase("published");

		obj =  dict.atIgnoreCase("obj");
		//"obj %".format(obj).debug(this);
		obj = MooObject.refToObject(obj, converter, moo);

		owner =  dict.atIgnoreCase("owner");
		owner = MooObject.refToObject(owner, converter, moo);

		^this.new(verb.asSymbol, dobj.asSymbol, iobj.asSymbol, func, object, publish.asBoolean, owner, moo);
	}

	*new {|verb, dobj, iobj, func, obj, publish, owner, moo|

		this.validID(verb).if({
			^super.newCopyArgs(verb, dobj, iobj, func, obj, owner).init(publish, moo);
		} , {
			MooError("Badly formed verb name").throw;
		});

	}

	init{ arg publish = false, moo;

		var name, id;

		owner = owner ? obj.owner;
		moo = moo ? obj.moo;
		id = MooObject.id(obj, moo);
		obj.isKindOf(MooObject).if({ name = obj.name }, { name = id });


		funcstr.isKindOf(String).not.if({
			funcstr = funcstr.asCompileString;
		});

		this.pass(funcstr).not.if({
			MooReservedWordError("Verb contains disallowed commands", this.check).throw;
		});

		{ funcstr.compile }.try({|err| MooCompileError(err.errorString).throw});

		func = SharedResource(funcstr);
		func.mountAPI(moo.api, "verb/%/%".format(id, verb).asSymbol, "% verb %".format(name, verb));


		publish.if({
			this.publish(moo, id, name);
		});
		published = publish;
	}

	pass {|str|

		str = str ? funcstr;
		^this.class.pass(str);
	}

	check{|str|
		// returns a list of problems
		var bad_words;

		str = str ? funcstr;

		bad_words= MooVerb.disallowed.collect({|naughty|
			funcstr.find(naughty, true).notNil.if({naughty});
		});

		bad_words.removeEvery([nil]);

		^bad_words;
	}

	publish {|moo, id, name|
		// add to the api
		id = id ? MooObject.id(obj);
		moo = moo ? obj.moo;
		name = name ?  obj.isKindOf(MooObject).if({ name = obj.name }, { name = id });

		moo.api.add("%/%".format(verb, id).asSymbol, {arg dob, iob; this.invoke(dob, iob)},
			"% % % % on %".format(verb, dobj, iobj, obj.name));
	}

	toJSON {|converter|

		^ "{ \"verb\": \"%\", ".format(verb) +
		"\"args\": [\"%\", \"%\"],".format(dobj, iobj) +
		"\"func\": %, ".format(converter.convertToJSON(func)) +
		"\"obj\" : %, ".format(converter.convertToJSON(obj)) +
		"\"published\": %, ".format(converter.convertToJSON(published)) +
		"\"owner\": % }" .format(converter.convertToJSON(owner));

	}


	verb {

		^verb.value

	}

	invoke {|dobj, iobj, caller, object|
		var str, f, clock;

		str = func.value;

		//"inovke".debug(verb);
		//str.debug(verb);

		this.pass(str).not.if({
			MooReservedWordError("Verb contains disallowed commands", this.check(str)).throw;
		});
		//str.postln;
		//"invoke".postln;
		f = str.compile.value; // "{|a| a.post}".compile.value returns a function

		//"compiled".debug(verb);

		object.notNil.if({
			clock = object.getClock;
		} , {
			caller.location.notNil.if({
				clock = caller.location.getClock;
			});
		});

		//"got clock".debug(verb);

		{f.value(dobj, iobj, caller, object);}.fork( * clock);

	}

	func {
		^func.value;
	}
}



+ String {

	isDecimal {

		var str, num = true;

		str = this.stripWhiteSpace;

		str.do({|char|

			((char == $\-) || ( char == $\. )  || (char.isDecDigit)).not.if({
				num = false;
				^num; // I know this is bad, but if it's a long string, whatevs
			});
		});

		^num;
	}



}