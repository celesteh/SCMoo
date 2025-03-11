
MooPermission {

	classvar <levels;
	var <level;

	*initClass {
		levels = [\user, \maker, \builder, \wizard, \root];
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

	asSymbol {|input|

		var output;

		input.isKindOf(SimpleNumer).if({
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

		input.isKindOf(SimpleNumer).not.if({
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
		^super.new.initRoot(moo, name);
	}

	initRoot {|moo, name|

		"MooRoot init".postln;
		name.postln;
		moo.dump;

		//moo = moo ? Moo.default;


		super.initPlayer(moo, name, nil, false);
		(id < 2).if({
			permissions = MooRootPermission();
		}, {
			MooPermissionsError("Not Root").throw;
		});
	}

}

MooPlayer  : MooObject {

	var contents, ownedObjects, <>user, <me, <permissions;

	*new { |moo, name, user, self=false|
		"MooPlayer.new".postln;
		^super.new.initPlayer(moo,name, user, self);
	}

	initPlayer {|imoo, iname, iuser, self=false|

		var pronouns;

		"initPlayer".postln;

		(imoo.notNil || iname.notNil || iuser.notNil).if({

		//imoo = imoo ? Moo.default;

		user = iuser;
		iname = iname ? user.notNil.if({ user.nick });

		me = self;
		me.if({
			iname = iname ? imoo.api.nick;
		});


		permissions = MooPermission();

		super.initMooObj(imoo, iname, this);
		owner = this;


		//owner = this;
		immobel = true;
		contents = [];
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

		this.verb_(\tell, \this, \any,  """
		{|dobj, iobj, caller|
            dobj.post(iobj.asString);
            }
"""
            );

		this.verb_(\say, \this, \any,  """
		{|dobj, iobj, caller|
				caller.location.announceExcluding(caller, \"% says, \\\"%\\\"\".format(caller.name, iobj.asString));
caller.post(\"You say,  \\\"%\\\"\".format(iobj.asString));
			}
		"""
		);
		this.verb_(\pose, \this, \any, """
			{|dobj, iobj, caller|
				caller.location.announce(\"% %\".format(caller.name, iobj.asString));
			}
"""
		);
		});
	}

	isPlayer{ ^true }

	post {|str|

		"in post".postln;
		me.if({
			str.postln;
		}, {
			// do the api call?

		});
	}

	postln {|str| this.post(str) }

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

	wizard {
		^(permissions.level >= \wizard);
	}

	permissions_ {|status, caller|

		// only a wizard can set a wizard
		caller.wizard.if({
			//wizard = status;
			permissions.level = status;
		}, {
			MooPermissionsError("% not a wizard".format(caller)).throw;
		});
	}

	login {


	}

}