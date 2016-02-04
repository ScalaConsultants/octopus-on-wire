# octopus-on-wire

![From http://bgott.blogspot.com/2005_06_01_archive.html](http://www.barrygott.com/blog/flamingocto.jpg)

# Scala event calendar widget

A JavaScript widget for displaying upcoming Scala events on your website in a fully customizable widget.

# About

- Public Name: Event Rocket
- Type: Growth, (possibly) Startup
- Status: Crawling, pre-MVP
- Goals for 2016: Deploy an MVP
- Description: A distributed network of widgets promoting technical events. Serves information to website visitors in a similar fassion as online adverts do (but without being intrusive) - using the reach of our affiliates
- Value to Scalac: Promoting our events, Gathering Data for Huntly

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
