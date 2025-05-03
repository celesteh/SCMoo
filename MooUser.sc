
MooPermission {

	classvar <levels;
	var <level;

	*initClass {
		levels = [\user, \musician, \maker, \builder, \wizard, \root];
	}

	*at{|index|

		index.isKindOf(SimpleNumber).if({
			^levels.at(index.asInteger);
		}, {
			^levels.indexOf(index.asSymbol);
		})
	}


	*new { ^super.new.init }

	init {

		level = 0;
	}

	*fromJSON {|str, converter|
		^super.new.fromJSON(str, converter);
	}

	fromJSON {|str, converter|

		var input;

		input = str.split(":").last;
		input = input.stripWhiteSpace;
		input = input.stripEnclosingQuotes;
		level = this.asInteger(input);
	}


	asSymbol {|input|

		var output;

		input = input ? level;

		"simple number? %".format(input.isKindOf(SimpleNumber)).debug(this);

		input.isKindOf(SimpleNumber).if({
			output =  MooPermission.at(input.asInteger);
		}, {
			output = input;
		});

		output.notNil.if({
			^output.asSymbol;
		}, {
			^nil
		});
	}


	asInteger {|input|

		var output;

		input = input ? level;

		input.isKindOf(SimpleNumber).not.if({
			output = MooPermission.at(input.asSymbol);
		}, {
			output = input;
		});

		output.notNil.if({
			^output.asInteger;
		}, {
			^nil
		});
	}


	level_{|status, caller|

		var newLevel;

		(caller.permissions >= \wizard).if ({
			newLevel = this.asInteger(status);
		});

		newLevel.notNil.if({
			level = newLevel;
		});
	}

	== {|comparison|

		comparison.isKindOf(MooPermission).not.if({
			comparison = this.asInteger(comparison);
		});

		^(this.level.asInteger == comparison.asInteger);
	}

	!= {|comparison|

		comparison.isKindOf(MooPermission).not.if({
			comparison = this.asInteger(comparison);
		});

		^(this.level.asInteger != comparison.asInteger);
	}

	> {|comparison|

		comparison.isKindOf(MooPermission).not.if({
			comparison = this.asInteger(comparison);
		});

		^(this.level.asInteger > comparison.asInteger);
	}

	>= {|comparison|

		comparison.isKindOf(MooPermission).not.if({
			comparison = this.asInteger(comparison);
		});

		^(this.level.asInteger >= comparison.asInteger);
	}

	< {|comparison|

		comparison.isKindOf(MooPermission).not.if({
			comparison = this.asInteger(comparison);
		});

		^(this.level.asInteger < comparison.asInteger);
	}

	<= {|comparison|

		comparison.isKindOf(MooPermission).not.if({
			comparison = this.asInteger(comparison);
		});

		^(this.level.asInteger <= comparison.asInteger);
	}


	toJSON {|converter|

		var sym;

		sym = this.asSymbol;
		sym.asString.debug(this);

		^"\"%\"".format(sym.asString);
	}

}


MooRootPermission : MooPermission {

	*new { ^super.new.initRoot }

	initRoot {

		level = MooPermission.at(\root);
	}


}


MooPermissionsError : MooError {}



MooRoot : MooPlayer {

	*new { |moo, name|
		"MooRoot.new".postln;
		name.postln;
		^super.new(moo, name, nil, false).initRoot(moo, name);
	}

	initRoot {|moo, name|

		"MooRoot init".postln;
		name.postln;
		moo.dump;

		//moo = moo ? Moo.default;


		//super.initPlayer(moo, name, nil, false);
		(this.id.asString.asInteger < 2).if({
			permissions = MooRootPermission();
		}, {
			MooPermissionsError("Not Root").throw;
		});
	}

	me_{|bool|
		me = bool
	}

	//id {
	//	^\0
	//}

	parent_ {|genericPlayer|

		var superID;

		moo.notNil.if({
			genericPlayer = genericPlayer ? moo.genericPlayer;

			"parent_".debug(this);

			genericPlayer.notNil.if({

				//	genericPlayer.isKindOf(MooObject).if({
				//		superID = genericPlayer.id;
				//		superObj = genericPlayer;
				//	}, {
				//		superID = genericPlayer;
				//		superObj = moo.at(superID);
				//	});
				//	this.property_(\parent, superID, false);
				//});

				this.pr_superObj_(genericPlayer);

				this.pr_copyParentProperties();
			});
		});
	}

}

MooPlayer  : MooContainer {

	//classvar >generic;
	var  ownedObjects, <>user, <me, <permissions;

	*new { |moo, name, user, self=false, parent|
		var uname;

		//"MooPlayer.new".postln;

		uname = name ? user.notNil.if({ user.nick });
		self.if({
			uname = uname ? moo.api.nick;
		});
		//*new { |moo, name, maker, parent|
		parent = this.generic(moo) ? parent;

		^super.new(moo, uname, \this, parent).initPlayer(user, self);
	}


	*fromJSON{|dict, converter, moo|
		"fromJSON MooPlayer".debug(this);
		^super.fromJSON(dict, converter, moo).playerRestore(dict, converter, moo);
	}

	initPlayer {|iuser, self=false|

		var pronouns;

		"initPlayer".postln;

		//(imoo.notNil || iname.notNil || iuser.notNil).if({

			//imoo = imoo ? Moo.default;

			user = iuser;
			//iname = iname ? user.notNil.if({ user.nick });

			me = self;
			//me.if({
			//	iname = iname ? imoo.api.nick;
			//});


			permissions = MooPermission();

			//super.initMooObj(imoo, iname, this, imoo.genericPlayer ? imoo.genericObject);
			owner = this;


			//owner = this;
			immobel = true;
			//contents = [];
			ownedObjects = [];
			//home = moo.lobby;
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

		this.verb_(\tell, \this, \any,
			{|dobj, iobj, caller, object|
				dobj.postUser(iobj.asString, caller);
			}.asCompileString;
		);

		this.verb_(\say, \this, \any,
			{|dobj, iobj, caller, object|
				caller.location.announceExcluding(caller, "% says, \"%\"".format(caller.name,
					dobj.asString.stripEnclosingQuotes), caller);
				caller.postUser("You say,  \"%\"".format(dobj.asString.stripEnclosingQuotes));
			}.asCompileString;
		);

		this.verb_(\pose, \this, \any,
			{|dobj, iobj, caller, object|
				caller.location.announce("% %".format(caller.name, dobj.asString.stripEnclosingQuotes));
			}.asCompileString;
		);

		this.verb_(\inventory, \this, \none,

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
					str.debug(object);
					caller.postUser(str);
				} , {
					"Should not be nil".warn;
				});
			}.asCompileString;

		);


		moo.api.add("postUser/%".format(this.id).asSymbol, { arg id, str;

			(id == this.id).if({
				this.postUser(str)
			});
		}, "For chatting. Usage: post/id, id, text");



		//});



	}

	isPlayer{ ^true }

	postUser {|str, caller|

		"in post".postln;
		// post is always local ARG NO IT's NOT . . . wait, is it? i don't fucking know....
		//me.if({
		//	str.postln;
		//}, {
			// do the api call? - only if we've originated the need for it
		((caller == this)||me).if({
				moo.api.sendMsg("post/%".format(this.id).asSymbol, this.id, str);
		},{
			"dont's post".debug(this);
		});
		//});
	}

	//postln {|str, caller| this.post(str, caller) }


	/*
	remove {|item|

		moo.semaphore.wait;
		contents.remove(item);
		moo.semaphore.signal

	}

	addObject {|item|

		moo.semaphore.wait;
		contents = contents.add(item);
		moo.semaphore.signal

	}
	*/

	wizard {
		^(permissions.level >= \wizard);
	}

	permissions_ {|status, caller|

		// only a wizard can set permissions
		caller.wizard.if({
			//wizard = status;
			permissions.level = status;
		}, {
			MooPermissionsError("% not a wizard".format(caller)).throw;
		});
	}

	login {


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

		var str, stuff;

		"MooPlayer.pr_JSONContents".debug(this);


		//stuff = contents.collect({|c| c !? { converter.convertToJSON(c.id) } ? "null" }).asList;

		str = super.pr_JSONContents(converter);

		//str = str + ",\"contents\":  % ," .format(converter.convertToJSON(stuff)/*stuff.join(", ")*/);
		str = str + ", \"owned\":  % ," .format(converter.convertToJSON(ownedObjects)/*stuff.join(", ")*/);
		str = str + "\"permission\": %".format(converter.convertToJSON(permissions));

		"ok, we got the MooUSer: %".format(str).debug(this);
		^str;
	}

	playerRestore {|dict, converter, moo|
		//		json_contents = dict.atIgnoreCase("contents");
		//"contents %".format(contents).debug(this);
		//contents = contents ++ json_contents.collect({|item| this.refToObject(item, converter, moo) });

		var json_owned, perms;

		"playerRestore".debug(this);


		semaphore = semaphore ? Semaphore(1);

		json_owned = dict.atIgnoreCase("owned");
		perms = dict.atIgnoreCase("permission");

		semaphore.wait;

		ownedObjects = [] ++ json_owned.collect({|item| this.class.refToObject(item, converter, moo) });
		permissions = MooPermission.fromJSON(perms, converter);

		semaphore.signal;

		"owned %".format(ownedObjects).debug(this);
	}

}
