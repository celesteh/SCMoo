
MooParser {

	var actor, input, verb, dobj, preposition, iobj;

	*new {|player, string|
		^super.new.init(player, string);
	}

	init{| speaker, string|

		var tokenised;

		actor = speaker;
		input = string;

		this.parse();

		"parsed".postln;

		"location % ".format(speaker.location).debug("MooParser init");

		this.movement().not.if({
			this.creation().not.if({
				this.call();
			});
		});
	}

	movement {

		var place, matched = false;

		"movement".postln;

		dobj.isNil.if({
			actor.location.notNil.if({
				place = actor.location.exit(verb);
				place.notNil.if({
					{ actor.move(place); }.fork;
					matched = true
				});
			});
		});
		^matched;
	}

	creation {

		var thing, matched = false, index;

		"creation".postln;

		(verb.asString.toLower.asSymbol == \make).if({
			matched = true;

			switch(dobj.toLower.asSymbol,
				\room, {
					thing = MooRoom(actor.moo, iobj, actor);
					actor.post("New room % is object number %".format(thing.name, thing.id));
					//{ actor.move(thing); }.fork;
				},
				\stage, {
					thing = MooStage(actor.moo, iobj, actor);
					thing.location = actor.location;
					{ actor.location.addObject(thing); }.fork;
				},
				\clock, {
					thing = MooClock(actor.moo, iobj, actor);
					//thing.location = actor.location;
					{ actor.addObject(thing); }.fork;
				},
				\object, {
					thing = MooObject(actor.moo, iobj, actor);
					{ actor.addObject(thing); }.fork;
				},
				{ matched = false; }
			);

		});

		(verb.asString.toLower.asSymbol == \exit).if({
			(actor.location.owner == actor).if({
				// exit north to 135
				index = iobj.asInteger;
				(index > 0).if({
					thing = actor.moo[index];
				} , {
					// not a number. Make a new room and connect it
					thing = MooRoom(actor.moo, iobj, actor);
					actor.post("New room % is object number %".format(thing.name, thing.id));
                });

                actor.location.addExit(dobj, thing);
                actor.post("New exit % to %".format(dobj.name, thing.name));
			    matched = true;
		     });
});



		^matched;
	}

	call {

		var d_obj, i_obj, vfunc, found, object;

		"verb: %".format(verb).postln;

		// try to match the dobj to an object
		dobj.notNil.if({
			d_obj = this.findObj(dobj);
		});

		// try to match the iobj to an object
		iobj.notNil.if({ i_obj = this.findObj(i_obj) });

		// is this a verb on the direct object?
		vfunc = this.checkObj(d_obj);
		object = d_obj;


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

				d_obj.isNil.if({
					d_obj = actor.location;

				},{
						i_obj.isNil.if({
							i_obj = actor.location
						});
				});
			});
		});

			// is it on the caller?
		vfunc.isNil.if({
			vfunc = this.checkObj(actor);
			vfunc.notNil.if({

				object = actor;

				d_obj.isNil.if({ d_obj = actor },
					{
						i_obj.isNil.if({
							i_obj = actor
						});
				});

			});
		});


		vfunc.notNil.if({ // call it!!!

			//vfunc.func.value(d_obj, i_obj, actor);
			vfunc.invoke(d_obj, i_obj, actor, object);

		});
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

		arr.postln;

		^arr;
	}

	pr_arrFlat{|arr, count|

		var assembled = [];

		[arr, count].postln;

		arr.do({|item|
			item.isKindOf(String).not.if({
				assembled = assembled ++ this.pr_arrFlat(item, count +1);
			}, {
				assembled = assembled.add(item);
			});
		});

		^assembled;
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
		});

		tokens = this.pr_arrFlat(tokens, 0);

		"flattened".postln;

		tokens.postln;

		verb = tokens[0];
		(tokens.size > 1).if({ dobj = tokens[1]; });

		(tokens.size > 2).if ({ iobj = tokens.last; });

		tokens.postln;
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

		var obj, found = false;

		key = key.asSymbol;

		(key ==\me).if({
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
			actor.contents.do({|item|

				found = item.matches(key);
				obj = item;
			});
		});

		found.not.if ({
			actor.location.players.do({|item|

			//found = matchObj(key, item);
				found = item.matches(key);
				obj = item;
			});
		});
		found.not.if ({
			actor.location.contents.do({|item|

				//found = matchObj(key, item);
				found = item.matches(key);
				obj = item;
			});
		});

		found.not.if.({ obj = key });

		^obj

	}

}





MooVerb{

	classvar <disallowed;
	var verb, <dobj, <iobj, funcstr, <obj, func;

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
			"superPerform", "superPerformList", "multiChannelPerform"
		]
	}


	*new {|verb, dobj, iobj, func, obj, publish|

		^super.newCopyArgs(verb, dobj, iobj, func, obj).init(publish);
	}

	init{ arg publish = false;

		this.pass(funcstr).not.if({
			MooReservedWordError("Verb contains disallowed commands", this.check).throw;
		});

		{ funcstr.compile }.try({|err| MooCompileError(err.errorString).throw});

		func = SharedResource(funcstr);
		func.mountAPI(obj.moo.api, "verb/%/%".format(obj.id, verb).asSymbol, "% verb %".format(obj.name, verb));


		publish.if({
			this.publish;
		})
	}

	pass {|str|
		var pass, problem_count;

		str = str ? funcstr;

		problem_count = MooVerb.disallowed.collect({|naughty|
			str.find(naughty, true).isNil.if({0}, {1}) + // 1 for a match, 0 for no match
			str.find(naughty.reverse, true).isNil.if({0}, {1});
		}).sum;

		pass = (problem_count ==0);
		^pass;
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

	publish {
		// add to the api
			obj.moo.api.add("%/%".format(verb, obj.id).asSymbol, {arg dob, iob; this.invoke(dob, iob)},
			"% % % % on %".format(verb, dobj, iobj, obj.name));
	}

	verb {

		^verb.value

	}

	invoke {|dobj, iobj, caller, object|
		var str, f;

		str = func.value;

		"-----------------------------------------------\ninvoke\ncaller is %".format(caller.name.value).debug("verb");
		caller.dumpBackTrace;

		this.pass(str).not.if({
			MooReservedWordError("Verb contains disallowed commands", this.check(str)).throw;
		});
		str.postln;
		"invoke".postln;
		f = str.compile.value; // "{|a| a.post}".compile.value returns a function

		f.value(dobj, iobj, caller, object);

	}

	func {
		^func.value;
	}
}
