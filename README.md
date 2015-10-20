# octopus-on-wire

![From http://bgott.blogspot.com/2005_06_01_archive.html](http://www.barrygott.com/blog/flamingocto.jpg)

# Scala event calendar widget

A JavaScript widget for displaying upcoming Scala events on your website in a fully customizable widget.

# Usage

Firstly, copy the ```octopus-on-wire.js``` file located in the ```client/dist``` directory of this repository to your project.
Then, embed it in the ```head``` section of your HTML file.

```html
<script src="octopus-on-wire.js"></script>
```

You can use the default stylesheet, which is ```client/dist/octopus-on-wire.min.css```, or provide your own.

To embed the widget in e.g. a HTML element with a "root" id, add this line at the bottom of your body element.

```html
<script>io.scalac.octopus.client.OctopusClient().buildWidget(document.getElementById("root"))</script>
```

That's it - the widget will be created in your root element.