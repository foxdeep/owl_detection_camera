import 'package:flutter/material.dart';
import 'package:owl_detection_camera/owl_detection_camera.dart';
import 'package:owl_detection_camera_example/preview.dart';

void main()
{
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatefulWidget
{
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp>  with WidgetsBindingObserver
{

  @override
  void initState()
  {
    super.initState();
    WidgetsBinding.instance!.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance!.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state)
  {
    //It's not calling at first.
    print("didChangeAppLifecycleState $state");

    if (state == AppLifecycleState.resumed) //It need call resume event in Android version.
        {
      if(Theme.of(context).platform == TargetPlatform.android)
      {
        OwlDetectionCamera?.onResumeEvent();
      }
    }
    else if(state == AppLifecycleState.paused) //It need call pause event in Android version.
        {
      if(Theme.of(context).platform == TargetPlatform.android)
      {
        OwlDetectionCamera?.onPauseEvent();
      }
    }
    super.didChangeAppLifecycleState(state);
  }

  @override
  Widget build(BuildContext context)
  {
    return MaterialApp(
        home: Preview()
    );
  }
}
