# whiteboard

A [re-frame](https://github.com/day8/re-frame) proejct.

## About
This is a whiteboard web app. The goal is to be able to draw, mark pictures, and collaborate. This is currently not an open source project. Master branch is deployed to https://master.d3ox20jskdfs09.amplifyapp.com/ for an example. 

### Project Overview
This project uses SVG canvas for drawing. Mouse and keyboard events are translated to effects and then custom shape handlers will handle the lifecycle of the shape. For example a free form line appends the xy coordinates starting at mouse down and stopping at mouse up while a text will have an active item until blur or mouse down again.

### Todos
Tons of them. This is very green. Lots of organizing and cleanup and feature development left to do.
