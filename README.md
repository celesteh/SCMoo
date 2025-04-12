# SCMoo
A MOO written in sclang

Relies on BiLETools and JSONlib

    { m = Moo.bootstrap(NetAPI.broadcast("foo", "bar")); }.fork

    (
    f = File("/tmp/Moo.JSON".standardizePath, "w");
    f.write(m.toJSON);
    f.close;
    )


    // load the file

    (
    {
	  n = NetAPI.broadcast("foo", "bar");
	  5.wait;
	  m = Moo.bootstrap(n, "/tmp/Moo.JSON".standardizePath);
    }.fork;
  )
