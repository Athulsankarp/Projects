# CNC Simulator

## Overview

The CNC Simulator is a JavaFX-based application that provides a graphical simulation of a CNC machine, allowing users to input G-code commands and visualize their execution in a 3D environment.&#x20;

## Features

- **G-code Editor:** Enter and execute G-code commands.
- **3D CNC Visualization:** View tool movement in a simulated CNC environment using JavaFX 3D.
- **Execution Control:** Run G-code step-by-step or execute all commands at once.
- **Adjustable Camera Controls:** Zoom, pan, and rotate the view for better visualization.

## Usage Guide

1. Enter G-code commands in the text area.
2. Use the **Run All** button to execute the entire script.
3. Use the **Step** button to execute line-by-line.
4. Click **Reset** to reposition the tool to the home position.
5. Drag the 3D view to adjust perspective and use scroll to zoom.

### Supported G-code Commands

| Command | Description             |
| ------- | ----------------------- |
| `G90`   | Absolute positioning    |
| `G91`   | Relative positioning    |
| `Xn`    | Move tool to X position |
| `Yn`    | Move tool to Y position |
| `Zn`    | Move tool to Z position |
| `Fnn`   | Set feed rate (speed)   |

## Future Enhancements

- Implement OpenGL (LWJGL) for advanced rendering.
- Support additional G-code commands.
- Add real-time toolpath visualization.
- Extend with a backend API for remote G-code execution.

## Contributing

Contributions are welcome! Feel free to fork the repository and submit pull requests.

## Author

AthulSankarp

