MooCustomEncoder {

	*new {
		^super.newCopyArgs()
	}

	value {|item, converter|

		//"running the encoder".debug(this);
		item.notNil.if({
			item.respondsTo(\toJSON).if({
				//"toJson incoming".debug(this);
				^item.toJSON(converter)
			});
		});
		^nil
	}
}


MooCustomDecoder {

	*new {
		^super.newCopyArgs()
	}

	value {|str, converter, class, moo|

		"running the decoder".debug(this);
		/*
		item.notNil.if({
			item.respondsTo(\fromJSON).if({
				"toJson incoming".debug(this);
				^item.fromJSON(converter)
			});
		});
		^nil
		*/
		class.asSymbol.asClass.findRespondingMethodFor(\fromJSON).notNil.if({
			^class.asSymbol.asClass.fromJSON(str, converter, moo);
		});

	}
}

MooJSONConverter : JSONlib {

	var queue, done, inTree;


	*new { |postWarnings = true, useEvent=true, customEncoder=nil, customDecoder=nil|
		^super.new.init(postWarnings, useEvent, customEncoder, customDecoder)
	}


	*convertToJSON {|object, customEncoder=nil, postWarnings=true|
		if((object.isKindOf(Dictionary) or: (object.isKindOf(SequenceableCollection))).not) {
			//Error("Can only convert a Dictonary/Event/Array to JSON but received %".format(object.class)).warn
			"Can only convert a Dictonary/Event/Array to JSON but received %".format(object.class).warn
		};
		customEncoder = customEncoder ? MooCustomEncoder();
		//"customEncoder %".format(customEncoder).debug(this);
		^super.new.init(postWarnings, customEncoder: customEncoder).prConvertTree(object)
	}

	*convertToSC {|json, customDecoder=nil, useEvent=false, postWarnings=true|
		var converter, raw;
		if(json.isKindOf(Symbol)) {
			json = json.asString;
		};

		if(json.isKindOf(String).not) {
			Error("Can only parse a String to JSON but received %".format(json.class)).throw
		};
		customDecoder = customDecoder ? MooCustomDecoder();
		converter = this.new(
			postWarnings,
			customDecoder: customDecoder,
			useEvent: useEvent
		);
		raw = converter.prConvertToSC(json.parseJSON);
		^converter.restoreMoo(raw);
	}


	*parseFile {|filePath, customDecoder=nil, useEvent=true, postWarnings=true|
		var converter, raw;
				customDecoder = customDecoder ? MooCustomDecoder();
		converter = this.new(
			postWarnings,
			customDecoder: customDecoder,
			useEvent: useEvent
		);
		raw = converter.prConvertToSC(filePath.parseJSONFile);
		^converter.restoreMoo(raw);
	}



	init {|postWarnings, useEvent, customEncoder, customDecoder|

		this.postWarnings = postWarnings;
		this.useEvent = useEvent;
		this.customEncoder = customEncoder;
		this.customDecoder = customDecoder;

		queue = [];
		done = [];
		inTree = false;

		//"init".debug(this);
	}

	convertToJSON{|object|

		//"convertToJson".debug(object);

		^this.prConvertToJson(object);
	}

	convertTree {|object, moo|

		var obj, id, collection;

		//"convertTree".debug(this);

		object.isKindOf(Collection).if({

			collection = object.select({|o| o.isKindOf(MooObject) });

			(collection.size > 0).if({
				(collection.size != object.size).if({
					"Mixed List! Only MooObjects are being exported".warn;
				});
				^this.prConvertTree(collection.asList);
			});

			//Not a collection containing MooObjects
			"This method is for MooObjects".warn;
			^this.prConvertToJson(object);

		});

		//object.isKindOf(SimpleNumber).if({
		(object.isKindOf(String) || object.isKindOf(Symbol)).if({
			obj = moo.at(object.asSymbol);
			obj.notNil.if({
				object = obj;
			});
		});

		object.isKindOf(MooObject).if({
			^this.prConvertTree(object, moo);
		});

		// Not a Moo object
		"This method is for MooObjects".warn;
		^this.prConvertToJson(object);
	}


	enqueue {|object|

		//"enqueue".debug(object);

		object.isKindOf(MooObject).if({
			queue = queue.add(object);
		}, {
			// something has gone wrong
			MooTypeError("Object is wrong type: %".format(object.class));
		});
	}


	prConvertTree {|object, moo|

		var str = "", obj, id, prev;

		moo = moo ? Moo.default;

		//"prConvertTree".debug(this);

		(inTree.not && object.isKindOf(MooObject)).if({
			inTree = true;
			//"inTree".debug(this);
			done = [object.id];
			str = "[" + object.toJSON(this) ;

			//"queue".debug(this);
			queue.do({|item| item.postln });
			//str.debug(this);

			obj = queue.pop;//queue.removeAt(0);

			{obj.notNil}.while({
				obj.isKindOf(MooObject).if({
					id = obj.id;
				}, {
					id = obj;
					//id.isKindOf(SimpleNumber).not.if({
					(id.isKindOf(String) || id.isKindOf(Symbol)).not.if({
						//obj.dump;
						//prev.notNil.if({
						//	"prev was % %".format(prev.class, prev.name).debug(this);
						//});
						//str.debug(this);
						Error("obj is % % % %".format(id.class, id, id.value.class, id.value)).throw;
					});
					obj = moo.at(id.asSymbol);
				});

				(obj.notNil && done.includes(id).not).if({
					str = str ++ "\n," + obj.toJSON(this);
					done = done ++ id;
				});

				prev = obj;
				obj = queue.pop;
				//obj = queue.removeAt(0);
			});

			//"done with tree".debug(this);

			str = str + "]";
			//^str;
			inTree = false;
		}, {


			(inTree.not && object.isKindOf(Collection)).if({
				//"get into a tree".debug(this);
				//queue = object.select({|o| o.isKindOf(MooObject) }).asList;

				object.select({|o| o.isKindOf(MooObject) }).asList.do({|o| this.enqueue(o) });

				obj = queue.pop;//queue.removeAt(0);

				//queue.debug(this);
				//obj.dump;

				obj.notNil.if({
					//"recursion".debug(this);
					str = this.prConvertTree(obj, moo);
				}, {
					//"failed to recurr".debug(this);
					str = this.prConvertToJson(object);
				});

			} , {
				//"it's just an object".debug(this);
				str = this.prConvertToJson(object);
			});
		});

		^str;
	}

	prConvertToJson{|object|

		var val, id, ret;

		//"prConvertToJson".debug(this);
		// find the player!


		object.isKindOf(MooObject).if({
			//"MooObject % %".format(object.class, object.name).debug(this);

			id = object.id;
			done.includes(id).not.if({
				//queue = queue ++ object;
				this.enqueue(object);
			});

			^"{ \"type\": \"ref\", \"id\": % }".format(super.prConvertToJson(id));
		} , {

			//"Not MooObject %".format(object.class).debug(this);

			if(customEncoder.notNil) {
				//"customEncoder is not nil".debug(this);
				val = customEncoder.value(object, this);
				//"val is %".format(val).debug(this);

				if(val.notNil) {
					^val
				}
			};

			object.isKindOf(List).if({
				object = object.asArray;
			});

			ret = super.prConvertToJson(object);

			//"returning %".format(ret).debug(this);

			^ret;
		});

	}

	restoreMoo{|obj, moo|

		var class;

		obj.isKindOf(Dictionary).if({

			class = obj.atIgnoreCase("class");

			class.notNil.if({
				^this.prConvertToMoo(obj, class, moo);
			});
		}, {
			obj.isKindOf(Collection).if({ // a dictionary is a kind of a collection!

				^obj.collect({|o| this.restoreMoo(o, moo); });
			});
		});

		^obj;
	}

	prConvertToMoo { |obj, class, moo|
		var res, val;
		if(customDecoder.notNil) {
			val = customDecoder.value(obj, this, class, moo);
			if(val.notNil) {
				^val
			}
		};

		^super.prConvertToSC(obj);
	}

}


+ SharedResource {
	toJSON {|converter|
		//"toJSON".debug(this);
		^converter.convertToJSON(this.value.value)
	}
}

+ SharedCV  {
	toJSON {|converter|
		//"toJSON %".format(this.value.value).debug(this);
		^converter.convertToJSON(this.value.value)
	}
}

+ String {
	stripEnclosingQuotes {
		var start, end;
		//var reject, str = this.stripWhiteSpace;
		//reject = {|s| s.reject({|c, i| ((c == $\") && (i==0)) }); }
		//^reject.(reject.(str).reverse).reverse;
		//var start=0, end, str = this.stripWhiteSpace, quotes=[$\',$\"];
		//end = str.size-1;
		//while({ quotes.includes(str[start])},{start=start+1});
		//while({ quotes.includes(str[end])},{end=end-1});
		//^this.copyRange(start,end);
		start = 0;
		(this[0] == $\").if({ start = 1 });
		end = this.size - 1;
		(this[end] == $\").if({ end = end -1});
		^this.copyRange(start, end);
	}


}

+ SharedSynthDef {
	toJSON{|converter|

		^ "{ \"class\": \"SharedSynthDef\"," +
		"\"compileString\": %, ".format(this.asCompileString) +
		"\"owner\": % }".format(converter.convertToJSON(this.owner));
	}
}

+ SharedSynthDefs {
	toJSON{|converter|
		// ok, we only want to save trusted synthdefs
		var toExport = local ++ approved;
		^ converter.convertToJSON(toExport);
	}
}


+ Dictionary {

	atIgnoreCase{ |key|

		var output, allKeys, distance, count, tie, best, firstLetter, index;

		output = this.associationAt(key.asSymbol).value;

		key = key.asString;
		this.isKindOf(IdentityDictionary).not.if({
			output = output ? this.associationAt(key).value;
			output = output ? this.associationAt(key.stripWhiteSpace).value;
		});

		output = output ? this.associationAt(key.stripWhiteSpace.asSymbol).value;

		output.isNil.if({
			allKeys = this.keys.select({|akey| akey.asString.compare(key, true) == 0 }).asList;

			// if there's just one result, we found it
			(allKeys.size == 1).if({
				//"one result".debug(this);
				output = this.associationAt(allKeys[0]).value;
				output.isNil.if({
					output = this.associationAt(allKeys[0].asSymbol).value;
				});
			});

			// if there's more than one result, pick the closest
			(allKeys.size > 1).if({
				// return the closest
				distance = Dictionary();
				allKeys.do({|akey|
					// count the differences
					count = akey.asString.count({|char, index| char != key[index]});
					tie = distance.at(count);
					tie.notNil.if({
						tie.isKindOf(Collection).if({
							tie = tie ++ akey;
						} , {
							tie = [tie, akey];
						});
						distance.put(count, tie);
					} , {
						distance = distance.put(count, akey);
					});
				});

				// sort the keys
				best = distance.keys.sort[0];
				best = distance.at(best);

				// is it a tie?
				best.isKindOf(Collection).if({

					//If one has a capitalisation in the first letter and that's the difference, it wins
					firstLetter = best.select({|akey|
						akey.asString;
						akey[0] != key[0]
					});
					// ok pick one
					(firstLetter.size > 0).if({
						index = firstLetter.choose;
					});
					// fine pick randomly
					index = best.choose

				});

				index = best;
							index.debug(this);
			output = this.associationAt(index);
			output.key.isNil.if({
				output = this.associationAt(index.asSymbol)
			});
			//^( ? this.associationAt(index.asSymbol));
			^output.value;

			});
		});

		// we have it or it's nil
		^output
	}
}